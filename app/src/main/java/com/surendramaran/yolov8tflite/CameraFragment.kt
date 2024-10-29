package com.surendramaran.yolov8tflite

// Import statements
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.surendramaran.yolov8tflite.Constants.LABELS_PATH
import com.surendramaran.yolov8tflite.Constants.MODEL_PATH
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Locale

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
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
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
            val bitmapBuffer =
                Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer,
                0,
                0,
                bitmapBuffer.width,
                bitmapBuffer.height,
                matrix,
                true
            )
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

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
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

            val bitmap = binding.viewFinder.bitmap
            if (bitmap != null) {
                for (box in boundingBoxes) {
                    val croppedBitmap = cropBitmap(bitmap, box)
                    runOCR(croppedBitmap, box.clsName) { expiryDate ->
                        Log.d("CameraFragment", "Detected expiry date: $expiryDate")
                        // Call getBoxColor with the extracted expiry date
                        val boxColor = getBoxColor(expiryDate)
                        box.color = boxColor // Assuming BoundingBox has a color property
                        binding.overlay.invalidate() // Refresh the overlay to show updated colors
                    }
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

    private fun runOCR(
        croppedBitmap: Bitmap,
        clsName: String,
        onExpiryDateDetected: (String) -> Unit
    ) {
        val image = InputImage.fromBitmap(croppedBitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedExpiryDates = mutableListOf<String>()
                val detectedQuantities = mutableListOf<String>()

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
                val expiryDateText = if (detectedExpiryDates.isNotEmpty()) {
                    detectedExpiryDates.firstOrNull() ?: "Expiry Date not detected"
                } else {
                    "Expiry Date not detected"
                }
                binding.expiryDate.text = "Expiry Date: $expiryDateText"

                // Call getBoxColor with the extracted expiry date
                val boxColor = getBoxColor(expiryDateText) // Pass the expiry date
                // Here you may want to set the box color to the bounding boxes if applicable
                // Assuming you want to apply this to each bounding box
                boundingBoxes.forEach { box ->
                    box.color = boxColor // Set the color based on the expiry date
                }
                binding.overlay.invalidate() // Refresh the overlay to show updated colors

                // Pass values to EditInventoryFragment when complete preview button is pressed
                binding.stopButton.setOnClickListener {
                    val bundle = Bundle().apply {
                        putString("itemName", clsName) // Pass clsName to EditInventoryFragment
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

    private fun getBoxColor(expiryText: String): Int {
        // Sanitize the expiry text
        val sanitizedText = expiryText.replace("\n", "").trim().replace(Regex("\\s+"), " ")

        // Log the sanitized text
        Log.d("CameraFragment", "Sanitized expiry text: '$sanitizedText'")

        return try {
            // Try parsing using DateTimeFormatter
            val formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)
            val expiryDate = LocalDate.parse(sanitizedText, formatter)
            val currentDate = LocalDate.now()

            when {
                expiryDate.isBefore(currentDate) -> Color.RED // Expired
                expiryDate.isBefore(currentDate.plusMonths(6)) -> Color.YELLOW // Near expiry (3-6 months)
                else -> Color.GREEN // Not expired (6+ months)
            }
        } catch (e: DateTimeParseException) {
            // Log the error
            Log.e("CameraFragment", "Failed to parse date using DateTimeFormatter: '$sanitizedText'", e)

            // Try manual parsing using getMonthIndex
            val parts = sanitizedText.split(" ")
            if (parts.size == 2) {
                val month = parts[0]
                val year = parts[1].toIntOrNull()

                // Get the month index
                val monthIndex = getMonthIndex(month)
                if (monthIndex != -1 && year != null) {
                    // Create the LocalDate using the month index and year
                    val expiryDate = LocalDate.of(year, monthIndex + 1, 1) // Use the first day of the month
                    val currentDate = LocalDate.now()

                    when {
                        expiryDate.isBefore(currentDate) -> Color.RED // Expired
                        expiryDate.isBefore(currentDate.plusMonths(6)) -> Color.YELLOW // Near expiry (3-6 months)
                        else -> Color.GREEN // Not expired (6+ months)
                    }
                } else {
                    // Log and return a default color if parsing fails
                    Log.e("CameraFragment", "Invalid month/year format: '$sanitizedText'")
                    Color.GRAY // Default color for unknown expiry
                }
            } else {
                // Log and return a default color if parts don't match expected format
                Log.e("CameraFragment", "Unexpected format for expiry date: '$sanitizedText'")
                Color.GRAY // Default color for unknown expiry
            }
        }
    }

    private fun getMonthIndex(month: String): Int {
        return when (month.toUpperCase()) {
            "JAN" -> Calendar.JANUARY
            "FEB" -> Calendar.FEBRUARY
            "MAR" -> Calendar.MARCH
            "APR" -> Calendar.APRIL
            "MAY" -> Calendar.MAY
            "JUN" -> Calendar.JUNE
            "JUL" -> Calendar.JULY
            "AUG" -> Calendar.AUGUST
            "SEP" -> Calendar.SEPTEMBER
            "OCT" -> Calendar.OCTOBER
            "NOV" -> Calendar.NOVEMBER
            "DEC" -> Calendar.DECEMBER
            else -> -1
        }
    }




}