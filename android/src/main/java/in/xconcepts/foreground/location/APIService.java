package in.xconcepts.foreground.location;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class APIService {
    private static final String TAG = "APIService";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds
    private static final int MAX_FAILED_BUFFER_SIZE = 1000; // Prevent memory issues
    private static final long CIRCUIT_BREAKER_TIMEOUT = 300000; // 5 minutes
    private static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5;
    private static final int BATCH_SIZE = 100; // Maximum points per API call

    private Context context;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // API Configuration
    private String apiUrl;
    private String apiType; // GET, POST, PUT, PATCH
    private Map<String, String> headers;
    private JSONObject additionalParams;
    private long apiIntervalMs;
    
    // Data Management
    private List<JSONObject> locationDataBuffer;
    private List<JSONObject> failedDataBuffer;
    private Runnable apiTask;
    private boolean isRunning = false;

    // Circuit breaker pattern
    private boolean circuitBreakerOpen = false;
    private long circuitBreakerOpenTime = 0;
    private int consecutiveFailures = 0;

    public APIService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.locationDataBuffer = new ArrayList<>();
        this.failedDataBuffer = new ArrayList<>();
        this.headers = new HashMap<>();
    }

    public void configure(String url, String type, Map<String, String> headers, 
                         JSONObject additionalParams, long intervalMinutes) {
        this.apiUrl = url;
        this.apiType = type != null ? type.toUpperCase() : "POST";
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.additionalParams = additionalParams;
        this.apiIntervalMs = intervalMinutes * 60 * 1000; // Convert minutes to milliseconds
        
        Log.d(TAG, "API Service configured - URL: " + url + ", Interval: " + intervalMinutes + " minutes");
    }

    public void startApiService() {
        if (isRunning || apiUrl == null || apiUrl.isEmpty()) {
            Log.w(TAG, "API Service already running or not configured properly");
            return;
        }

        isRunning = true;
        scheduleApiCall();
        Log.d(TAG, "API Service started");
    }

    public void stopApiService() {
        isRunning = false;
        if (apiTask != null) {
            mainHandler.removeCallbacks(apiTask);
        }
        
        // Send remaining data before stopping
        if (!locationDataBuffer.isEmpty() || !failedDataBuffer.isEmpty()) {
            executorService.execute(this::sendLocationData);
        }
        
        Log.d(TAG, "API Service stopped");
    }

    public synchronized void addLocationData(JSONObject locationData) {
        locationDataBuffer.add(locationData);
        Log.d(TAG, "Location data added to buffer. Buffer size: " + locationDataBuffer.size());
    }

    private void scheduleApiCall() {
        if (!isRunning) return;

        apiTask = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    executorService.execute(() -> {
                        sendLocationData();
                        if (isRunning) {
                            mainHandler.postDelayed(this, apiIntervalMs);
                        }
                    });
                }
            }
        };

        mainHandler.postDelayed(apiTask, apiIntervalMs);
        Log.d(TAG, "Next API call scheduled in " + (apiIntervalMs / 1000) + " seconds");
    }

    private void sendLocationData() {
        List<JSONObject> dataToSend = new ArrayList<>();
        
        synchronized (this) {
            // Limit failed buffer size to prevent memory issues
            if (failedDataBuffer.size() > MAX_FAILED_BUFFER_SIZE) {
                int removeCount = failedDataBuffer.size() - MAX_FAILED_BUFFER_SIZE;
                Log.w(TAG, "Failed buffer too large, removing " + removeCount + " oldest entries");
                failedDataBuffer.subList(0, removeCount).clear();
            }
            
            // Add failed data from previous attempts FIRST
            dataToSend.addAll(failedDataBuffer);
            failedDataBuffer.clear();
            
            // Add current buffer data
            dataToSend.addAll(locationDataBuffer);
            locationDataBuffer.clear();
        }

        if (dataToSend.isEmpty()) {
            Log.d(TAG, "No location data to send");
            return;
        }

        Log.d(TAG, "Attempting to send " + dataToSend.size() + " location points");
        
        // Split large payloads if necessary
        if (dataToSend.size() > BATCH_SIZE) {
            sendDataInBatches(dataToSend);
        } else {
            JSONObject requestBody = createRequestBody(dataToSend);
            boolean success = sendApiRequest(requestBody, 0);
            handleSendResult(success, dataToSend);
        }
    }

    private void sendDataInBatches(List<JSONObject> allData) {
        List<JSONObject> failedBatch = new ArrayList<>();
        
        for (int i = 0; i < allData.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, allData.size());
            List<JSONObject> batch = allData.subList(i, endIndex);
            
            JSONObject requestBody = createRequestBody(batch);
            boolean success = sendApiRequest(requestBody, 0);
            
            if (!success) {
                failedBatch.addAll(batch);
            }
        }
        
        handleSendResult(failedBatch.isEmpty(), failedBatch);
    }

    private void handleSendResult(boolean success, List<JSONObject> data) {
        if (!success && !data.isEmpty()) {
            synchronized (this) {
                failedDataBuffer.addAll(data);
            }
            Log.w(TAG, "API call failed, " + data.size() + " points added to retry buffer");
        } else if (success && !data.isEmpty()) {
            Log.d(TAG, "Successfully sent " + data.size() + " location points");
        }
    }

    private JSONObject createRequestBody(List<JSONObject> locationData) {
        JSONObject requestBody = new JSONObject();
        
        try {
            JSONArray locationArray = new JSONArray();
            for (JSONObject location : locationData) {
                locationArray.put(location);
            }
            
            requestBody.put("locationData", locationArray);
            
            if (additionalParams != null) {
                requestBody.put("additionalParams", additionalParams);
            } else {
                requestBody.put("additionalParams", JSONObject.NULL);
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request body", e);
        }
        
        return requestBody;
    }

    private boolean sendApiRequest(JSONObject requestBody, int retryAttempt) {
        // Check circuit breaker
        if (circuitBreakerOpen) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - circuitBreakerOpenTime < CIRCUIT_BREAKER_TIMEOUT) {
                Log.d(TAG, "Circuit breaker is open, skipping API call");
                return false;
            } else {
                // Try to close circuit breaker
                circuitBreakerOpen = false;
                consecutiveFailures = 0;
                Log.d(TAG, "Circuit breaker timeout expired, attempting to close");
            }
        }

        if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
            Log.e(TAG, "Max retry attempts reached");
            handleConsecutiveFailure();
            return false;
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            
            // Enhanced timeout settings
            connection.setRequestMethod(apiType);
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            connection.setUseCaches(false);
            
            // Set headers
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "ForegroundLocationPlugin/1.0");
            
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            
            // Send request body
            if (!"GET".equals(apiType)) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                    os.flush();
                }
            }
            
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "API Response Code: " + responseCode + " for " + 
                  requestBody.optJSONArray("locationData").length() + " points");
            
            if (responseCode >= 200 && responseCode < 300) {
                // Success - reset failure counter
                consecutiveFailures = 0;
                if (circuitBreakerOpen) {
                    circuitBreakerOpen = false;
                    Log.d(TAG, "Circuit breaker closed after successful request");
                }
                
                String response = readResponse(connection);
                Log.d(TAG, "API Success Response: " + response);
                return true;
            } else {
                // Handle different error codes
                String errorResponse = readErrorResponse(connection);
                Log.e(TAG, "API Error Response (" + responseCode + "): " + errorResponse);
                
                return handleHttpError(responseCode, requestBody, retryAttempt);
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Network error during API call (attempt " + (retryAttempt + 1) + ")", e);
            return handleNetworkError(requestBody, retryAttempt);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean handleHttpError(int responseCode, JSONObject requestBody, int retryAttempt) {
        // Determine if we should retry based on error code
        boolean shouldRetry = false;
        
        switch (responseCode) {
            case 408: // Request Timeout
            case 429: // Too Many Requests
            case 500: // Internal Server Error
            case 502: // Bad Gateway
            case 503: // Service Unavailable
            case 504: // Gateway Timeout
                shouldRetry = true;
                break;
            case 401: // Unauthorized
            case 403: // Forbidden
                Log.e(TAG, "Authentication/Authorization error - check API credentials");
                handleConsecutiveFailure();
                return false;
            case 400: // Bad Request
            case 422: // Unprocessable Entity
                Log.e(TAG, "Client error - request format issue");
                return false;
            default:
                if (responseCode >= 500) {
                    shouldRetry = true;
                }
                break;
        }
        
        if (shouldRetry && retryAttempt < MAX_RETRY_ATTEMPTS - 1) {
            long delay = calculateRetryDelay(retryAttempt, responseCode);
            Log.d(TAG, "Retrying API call in " + delay + "ms (attempt " + (retryAttempt + 1) + ")");
            
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            
            return sendApiRequest(requestBody, retryAttempt + 1);
        }
        
        handleConsecutiveFailure();
        return false;
    }

    private boolean handleNetworkError(JSONObject requestBody, int retryAttempt) {
        if (retryAttempt < MAX_RETRY_ATTEMPTS - 1) {
            long delay = calculateRetryDelay(retryAttempt, 0);
            Log.d(TAG, "Retrying API call after network error in " + delay + "ms");
            
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
            
            return sendApiRequest(requestBody, retryAttempt + 1);
        }
        
        handleConsecutiveFailure();
        return false;
    }

    private long calculateRetryDelay(int retryAttempt, int responseCode) {
        // Base delay with exponential backoff
        long baseDelay = RETRY_DELAY_MS * (long) Math.pow(2, retryAttempt);
        
        // Add jitter to prevent thundering herd
        long jitter = (long) (Math.random() * 1000);
        
        // Special handling for rate limiting
        if (responseCode == 429) {
            baseDelay = Math.max(baseDelay, 60000); // Minimum 1 minute for rate limits
        }
        
        return baseDelay + jitter;
    }

    private void handleConsecutiveFailure() {
        consecutiveFailures++;
        Log.w(TAG, "Consecutive failures: " + consecutiveFailures);
        
        if (consecutiveFailures >= CIRCUIT_BREAKER_FAILURE_THRESHOLD) {
            circuitBreakerOpen = true;
            circuitBreakerOpenTime = System.currentTimeMillis();
            Log.w(TAG, "Circuit breaker opened due to consecutive failures");
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String readErrorResponse(HttpURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (IOException e) {
            return "Unable to read error response";
        }
    }

    public int getBufferSize() {
        return locationDataBuffer.size() + failedDataBuffer.size();
    }

    public void clearBuffers() {
        synchronized (this) {
            locationDataBuffer.clear();
            failedDataBuffer.clear();
        }
        Log.d(TAG, "All buffers cleared");
    }

    public boolean isApiHealthy() {
        return !circuitBreakerOpen && consecutiveFailures < CIRCUIT_BREAKER_FAILURE_THRESHOLD;
    }

    public void resetCircuitBreaker() {
        circuitBreakerOpen = false;
        consecutiveFailures = 0;
        circuitBreakerOpenTime = 0;
        Log.d(TAG, "Circuit breaker manually reset");
    }

    public void shutdown() {
        stopApiService();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
