package com.liliankasem.smile;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;


public class MainActivity extends Activity {

    public static final String imageName = "face.jpeg";
    public static final String storageContainer = "dateface";
    //change
    public static final String storageConnectionString =
            "DefaultEndpointsProtocol=http;" +
                    "AccountName=lilian;" +
                    "AccountKey=SJNic/QAlKQXpfmLQPaW69v+wfTqqoKA7yHatPYI1JQiT/EXF9RheB0mxCxBfEItoSTxxlID1jY1fjze3Ozzpw==";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GetEmotion getEmotion = new GetEmotion();
        getEmotion.execute("path");
    }




    /** Projext Oxford logic */

    @SuppressWarnings("deprecation")
    private class GetEmotion extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {

            //Store image in blob storage
            storeImageInBlobStorage(params[0]);

            //Get the happiness score from API
            String result = getEmotionScore();

            return result;
        }

        @Override
        protected void onPostExecute(String result) {

            double happiness = Double.parseDouble(result);

            Log.i("TAG", result);

            if(happiness > 0.5){
                Log.i("TAG", "Is Smiling");

                Toast.makeText(getApplicationContext(), "Smile!", Toast.LENGTH_SHORT).show();

//                Intent translucent = new Intent(TakePicture.this, MainActivity.class);
//                translucent.putExtra("ImageListIterator", i);
//                translucent.putExtra("Result", "like");
//                translucent.putExtra("CallMain", true);
//                startActivity(translucent);

            }

        }

        protected void storeImageInBlobStorage(String imgPath){
            try
            {
                // Retrieve storage account from connection-string.
                CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

                // Create the blob client.
                CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

                // Retrieve reference to a previously created container.
                CloudBlobContainer container = blobClient.getContainerReference(storageContainer);

                // Create or overwrite the "face.jpeg" blob with contents from a local file.
                CloudBlockBlob blob = container.getBlockBlobReference(imageName);
                File source = new File(imgPath);
                blob.upload(new FileInputStream(source), source.length());
            }
            catch (Exception e)
            {
                // Output the stack trace.
                e.printStackTrace();
            }
        }

        protected String getEmotionScore(){

            int status = 0;
            String response = "";
            String happinessScore = "";
            String apiURL = "https://api.projectoxford.ai/emotion/v1.0/recognize";
            String apiParam = "https://lilian.blob.core.windows.net/" + storageContainer + "/" + imageName;


            try{
                URL url = new URL(apiURL);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Ocp-Apim-Subscription-Key", "" + "0ab219fb0a6741e19161ff8558028062");
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setDoInput(true);
                conn.setDoOutput(true);

                Uri.Builder builder = new Uri.Builder().appendQueryParameter("url", apiParam);
                String query = builder.build().getEncodedQuery();

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

                writer.write(query);
                writer.flush();
                writer.close();

                os.close();
                conn.connect();

                status = conn.getResponseCode();

                switch (status) {
                    case 200:
                    case 201:
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line+"\n");
                        }
                        br.close();

                        response = sb.toString();
                        JSONArray resultJson = new JSONArray(response);
                        JSONObject mainJson = resultJson.getJSONObject(0);
                        JSONObject scores = mainJson.getJSONObject("scores");
                        happinessScore = scores.getString("happiness");

                        return happinessScore;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }catch (JSONException e) {
                e.printStackTrace();
            }

            return happinessScore;
        }

    }
}
