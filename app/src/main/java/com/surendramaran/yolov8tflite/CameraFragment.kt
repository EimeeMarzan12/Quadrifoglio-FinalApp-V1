package com.surendramaran.yolov8tflite

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.surendramaran.yolov8tflite.Constants.LABELS_PATH
import com.surendramaran.yolov8tflite.Constants.MODEL_PATH
import com.surendramaran.yolov8tflite.Constants.LABELS2_PATH
import com.surendramaran.yolov8tflite.Constants.MODEL2_PATH
import com.surendramaran.yolov8tflite.databinding.FragmentCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class CameraFragment : Fragment(), Detector.DetectorListener {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private val isFrontCamera = false
    private lateinit var detector2: Detector
    private lateinit var detector: Detector

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService
    private var boundingBoxesModel1: List<BoundingBox> = emptyList()
    private var boundingBoxesModel2: List<BoundingBox> = emptyList()

    // Initialize the text recognizer for OCR
    private lateinit var textRecognizer: com.google.mlkit.vision.text.TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the text recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Set up detectors: One for model.tflite and one for model2.tflite
        detector = Detector(requireContext(), MODEL_PATH, LABELS_PATH, this)
        detector.setup()

        detector2 = Detector(requireContext(), MODEL2_PATH, LABELS2_PATH, this)
        detector2.setup()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
            detector.detect(rotatedBitmap, 1)  // Classification using model.tflite
            detector2.detect(rotatedBitmap, 2)  // OCR using model2.tflite
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) { startCamera() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detector.clear()
        cameraExecutor.shutdown()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(Manifest.permission.CAMERA).toTypedArray()
    }

    override fun onEmptyDetect() {
        binding.overlay.invalidate()
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        boundingBoxesModel1 = boundingBoxes // Store results for model 1 (Classification)

        requireActivity().runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.setResults(boundingBoxesModel1, boundingBoxesModel2) // Pass both sets of results
            binding.overlay.invalidate()

            // Display the classification result from `model.tflite`
            for (box in boundingBoxes) {
                Log.d(TAG, "Classification Result: ${box.clsName}")
            }
        }
    }

    override fun onDetect2(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        boundingBoxesModel2 = boundingBoxes // Store results for model 2 (OCR)

        requireActivity().runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms for Model 2"
            binding.overlay.setResults(boundingBoxesModel1, boundingBoxesModel2) // Pass both sets of results
            binding.overlay.invalidate()

            // For each bounding box, crop the region and run OCR for expiration date and quantity
            binding.viewFinder.bitmap?.let { bitmap ->
                for (box in boundingBoxes) {
                    val croppedBitmap = cropBitmap(bitmap, box) // Crop using detected box
                    runOCR(croppedBitmap) // Run OCR on the cropped region
                }
            } ?: Log.e(TAG, "Bitmap is null, skipping OCR")
        }
    }

    // Function to crop bitmap using detected bounding box
    private fun cropBitmap(bitmap: Bitmap, box: BoundingBox): Bitmap {
        val left = box.left.toInt()
        val top = box.top.toInt()
        val width = box.width.toInt()
        val height = box.height.toInt()

        // Validate dimensions before cropping
        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Invalid bounding box dimensions")
            return bitmap // Return the original bitmap in case of invalid dimensions
        }
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    // Function to run OCR on cropped bitmap
    private fun runOCR(croppedBitmap: Bitmap) {
        val image = InputImage.fromBitmap(croppedBitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                for (block in visionText.textBlocks) {
                    Log.d("OCR", "Detected text: ${block.text}")
                }
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "OCR failed: ${e.message}")
            }
    }
}
