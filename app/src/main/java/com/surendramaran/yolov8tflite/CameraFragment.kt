package com.surendramaran.yolov8tflite

// Import statements
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
import com.surendramaran.yolov8tflite.databinding.FragmentCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.surendramaran.yolov8tflite.Constants.LABELS_PATH
import com.surendramaran.yolov8tflite.Constants.MODEL_PATH

class CameraFragment : Fragment(), Detector.DetectorListener {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!! // Ensure binding is not null
    private val isFrontCamera = false
    private lateinit var detector: Detector
    private lateinit var textRecognizer: com.google.mlkit.vision.text.TextRecognizer

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService
    private var boundingBoxes: List<BoundingBox> = emptyList()

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

        // Set up the detector with the model
        detector = Detector(requireContext(), MODEL_PATH, LABELS_PATH, this)
        detector.setup()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
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

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
            detector.detect(rotatedBitmap)

            imageProxy.close()
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
        if (it[Manifest.permission.CAMERA] == true) {
            startCamera()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detector.clear()
        cameraExecutor.shutdown()
        _binding = null // Prevent memory leaks
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
        this.boundingBoxes = boundingBoxes

        requireActivity().runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.setResults(boundingBoxes) // Display results
            binding.overlay.invalidate()

            // Ensure bitmap is not null before proceeding
            val bitmap = binding.viewFinder.bitmap
            if (bitmap != null) {
                for (box in boundingBoxes) {
                    val croppedBitmap = cropBitmap(bitmap, box)
                    runOCR(croppedBitmap)
                }
            } else {
                Log.e(TAG, "Bitmap is null, skipping OCR")
            }
        }
    }


    private fun cropBitmap(bitmap: Bitmap, box: BoundingBox): Bitmap {
        val left = box.left.coerceIn(0f, bitmap.width.toFloat()).toInt()
        val top = box.top.coerceIn(0f, bitmap.height.toFloat()).toInt()
        val width = box.width.coerceIn(0f, (bitmap.width - left).toFloat()).toInt()
        val height = box.height.coerceIn(0f, (bitmap.height - top).toFloat()).toInt()

        return if (width > 0 && height > 0) {
            Bitmap.createBitmap(bitmap, left, top, width, height)
        } else {
            bitmap
        }
    }

    private fun runOCR(croppedBitmap: Bitmap) {
        val image = InputImage.fromBitmap(croppedBitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedQuantities = mutableListOf<String>()
                val detectedExpiryDates = mutableListOf<String>()

                for (block in visionText.textBlocks) {
                    Log.d("OCR", "Detected text: ${block.text}")

                    // Extract quantities and expiry dates as before
                    detectedQuantities.addAll(extractQuantities(block.text))
                    detectedExpiryDates.addAll(extractExpiryDates(block.text))
                }

                // Update the UI with detected quantities
                binding.qty.text = if (detectedQuantities.isNotEmpty()) {
                    "Qty: ${detectedQuantities.firstOrNull()}"
                } else {
                    "Qty not detected"
                }

                // Update UI with detected expiry dates
                binding.expiryDate.text = if (detectedExpiryDates.isNotEmpty()) {
                    "Expiry Date: ${detectedExpiryDates.firstOrNull()}"
                } else {
                    "Expiry Date not detected"
                }

                // Pass values to EditInventoryFragment when complete preview button is pressed
                binding.stopButton.setOnClickListener {
                    val bundle = Bundle().apply {
                        putString("quantity", detectedQuantities.firstOrNull())
                        putString("expiry", detectedExpiryDates.firstOrNull())
                    }
                    val editInventoryFragment = EditInventoryFragment()
                    editInventoryFragment.arguments = bundle

                    // Navigate to EditInventoryFragment
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.main_fragment, editInventoryFragment) //
                        .addToBackStack(null)
                        .commit()
                }
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "OCR failed: ${e.message}")
            }
    }

    private fun extractQuantities(text: String): List<String> {
        val regex = Regex("\\b\\d+[\\s]*(ML|ml|TABLETS|Tablet|TB|tb)\\b")
        return regex.findAll(text).map { it.value }.toList()
    }

    private fun extractExpiryDates(text: String): List<String> {
        val regex = Regex("\\b\\w{3}\\s\\d{4}\\b") // e.g., "DEC 2027"
        return regex.findAll(text).map { it.value }.toList()
    }
}
