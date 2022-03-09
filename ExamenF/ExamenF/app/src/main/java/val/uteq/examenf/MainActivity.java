package val.uteq.examenf;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;


import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import val.uteq.examenf.Models.ClasificationResult;
import val.uteq.examenf.ml.ModelUnquant;
import val.uteq.examenf.utiles.Methods;
import val.uteq.examenf.utiles.MyLogs;


public class MainActivity extends AppCompatActivity {
    private int[] CODES = {50, 100, 200, 250};
    private boolean FilesPermit = true;
    private ImageView img;
    private int globalImageSize = 224; //240 width
    private JsonObject JsonCountry;



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermitStorage();

        img = this.findViewById(R.id.imageView);
        //obtener referencia del botón
        Button button = (Button) findViewById(R.id.btnsearchpicture);
        //asignar evento al botón
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (FilesPermit) {
                    //para cargar una nueva imagen
                    Intent gallery = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    //se ejecuta el intent
                    startActivityForResult(gallery, CODES[0]);
                } else {
                    MessageToast(MainActivity.this, "no tienes permiso");
                }
                //startActivity(gallery);
            }
        });
        Button buttonT = (Button) findViewById(R.id.btntakepicture);
        buttonT.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePic.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(takePic, CODES[2]);
                    }else {
                        MessageToast(MainActivity.this, "mal intent");
                    }
                } else {
                    MessageToast(MainActivity.this, "no tienes permiso");
                }
            }
        });

        Button buttonOpenMap = (Button) findViewById(R.id.changeview);
        buttonOpenMap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (JsonCountry != null) {
                    //llamar al metodo iniciar actividad con el mapita
                    strartMap(JsonCountry);
                }
            }
        });
    }

    public static void MessageToast(Context ctx, String message){
        Toast toast= Toast.makeText(ctx, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    /**
     * solicitar permisos
     */
    private void requestPermitStorage() {
        MessageToast(MainActivity.this, "No tiene permiso a archivos");
        //solicitar acceso a archivos del dispositivo
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                }, CODES[1]);
    }

    /**
     * Cardar un archivo desde la carpeta assets
     * @param filename - el nombre del archivo a cargar
     * @return - El contenido del archivo en una cadena de texto
     */
    public String loadFileFromAsset(String filename) {
        String text = null;
        try {
            InputStream is = this.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            text = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return text;
    }

    /**
     * Procesa la clasificación
     * @param self_image - el mapa de bits correspondiente a la imagen a procesar
     */
    public void ManageClasification(Bitmap self_image){
        MyLogs.info(" Infor---------------------------------------->");
        //Crea un array con los posibles elemeentos en respuesta a la clasificación
        List<ClasificationResult> elements;
        String textfile = loadFileFromAsset("labels.json");
        Gson gson = new Gson();

        Type type = new TypeToken<List<ClasificationResult>>() {}.getType();
        elements = gson.fromJson(textfile, type);

        //Llama a la función de clasificación de imagenes
        float[] confidences = classifyImage(self_image);
        //Asigna la confianza correspondiente a cada uno de los elementos cargads anteriormente
        for(int i = 0; i < confidences.length; i++){
            boolean flagFind = false;
            for(ClasificationResult element: elements){
                if(element.getIdentifier() == i){
                    element.setPrecision(confidences[i] * 100);
                    flagFind = true;
                }
            }
            if(!flagFind){
                MyLogs.error("No se encontro elemento para: " + i);
            }
        }
        for (int i = 0; i < elements.size(); i++){
            MyLogs.info(elements.get(i).getIdentifier()+": "+ elements.get(i).getAlpha2code() + ", value: "+ elements.get(i).getPrecision());
        }
        //Ordenar los elementos de mayor a menos confianza
        MyLogs.info(" Sorted---------------------------------------->");
        elements = sortCards(elements);
        for (int i = 0; i < elements.size(); i++){
            MyLogs.info(elements.get(i).getIdentifier()+": "+ elements.get(i).getAlpha2code() + ", value: "+ elements.get(i).getPrecision());
        }
        if(elements.size() > 0) {
            getDataVolley(elements.get(0));
        }
    }

    public List<ClasificationResult> sortCards(List<ClasificationResult> elements){
        List<ClasificationResult> sorted = new ArrayList<>();
        while(elements.size() > 0) {
            int maxPosition = 0;
            float maxValue = elements.get(0).getPrecision();

            for (int i = 0; i < elements.size(); i++) {
                if (elements.get(i).getPrecision() > maxValue && i != maxPosition) {
                    maxPosition = i;
                    maxValue = elements.get(i).getPrecision();
                }
            }
            if (maxValue != -1) {
                ClasificationResult tmp = elements.remove(maxPosition);
                sorted.add(tmp);
            }
        }
        return sorted;
    }

    /*******************************************************************************************
     *                                  CLASIFICACION DE LA IMAGEN                                 *
     *******************************************************************************************/

    public float[] classifyImage(Bitmap self_image){
        float[] confidences = null;
        try {
            ModelUnquant model = ModelUnquant.newInstance(getApplicationContext());


            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);


            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * globalImageSize * globalImageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[globalImageSize * globalImageSize];
            self_image.getPixels(intValues, 0, self_image.getWidth(), 0, 0, self_image.getWidth(), self_image.getHeight());

            int pixel = 0;
            for (int i = 0; i < globalImageSize; i++){
                for (int j = 0; j < globalImageSize; j++){
                    int val = intValues[pixel++];
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            ModelUnquant.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            confidences = outputFeature0.getFloatArray();
            // Releases model resources if no longer used.
            model.close();




        } catch (IOException e) {
            MyLogs.error("Error tfLite: " + e.getMessage());
            confidences = null;
        }
        return confidences;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //verifica si el resultado de la petición fue satisfactorio
        if (resultCode == Activity.RESULT_OK) {
            //verifica si se ha seleccionado una imagen
            if (requestCode == CODES[0]) {
                //obtener imageUri
                Uri imageUri = data.getData();
                if (img != null) {
                    try {
                        //ubicar imagen en contenedor ImageView
                        //img.setImageURI(imageUri);

                        Bitmap imgBitMap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);


                        int dimension = Math.min(imgBitMap.getWidth(), imgBitMap.getHeight());
                        imgBitMap = ThumbnailUtils.extractThumbnail(imgBitMap, dimension, dimension);
                        img.setImageBitmap(imgBitMap);

                        imgBitMap = Bitmap.createScaledBitmap(imgBitMap, 224, 224, false);
                        ManageClasification(imgBitMap);
                    } catch (Exception ex) {
                        MyLogs.error("ImgSetUri: " + ex.getMessage());
                    }
                }
            } else if (requestCode == CODES[1]) {
                //obtiene respuesta de la imagen
                MessageToast(MainActivity.this, "Permiso aceptado");
                FilesPermit = true;
            } else if (requestCode == CODES[2]) {
                //obtiene respuesta de la imagen
                Bundle extras = data.getExtras();
                Bitmap imgBitMap = (Bitmap) extras.get("data");
                int dimension = Math.min(imgBitMap.getWidth(), imgBitMap.getHeight());
                imgBitMap = ThumbnailUtils.extractThumbnail(imgBitMap, dimension, dimension);
                img.setImageBitmap(imgBitMap);

                imgBitMap = Bitmap.createScaledBitmap(imgBitMap, 224, 224, false);
                ManageClasification(imgBitMap);

            }
        }
        MyLogs.error("resultCode: " + resultCode);
    }

    /*******************************************************************************************
     *                                  OBTENER LA INFORMACION DE LA API                       *
     *******************************************************************************************/
    private void getDataVolley(ClasificationResult param) {
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);

        StringRequest request = new StringRequest(
                Request.Method.GET,
                "http://www.geognos.com/api/en/countries/info/" + param.getAlpha2code() + ".json",
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        MyLogs.info("-----------------------");
                        //MyLogs.info(response);
                        countryProcessRequest(response, param);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        MessageToast(MainActivity.this, "Error en Volley");
                        MyLogs.info(error.getMessage());
                        //cierra el modal una vez haya cargado la api
                    }
                }
        );
        queue.add(request);
    }

    private void countryProcessRequest(String response, ClasificationResult element) {
        //convertir respuesta de volley en Json de google
        JsonObject jso = Methods.stringToJSON(response);
        MyLogs.info("Estado: " + Methods.JsonToString(jso, "StatusMsg", ""));
        //verificamos el estado de la petición [parámetro que viene del json]
        if (Methods.JsonToInteger(jso, "StatusCode", 400) == 200) {
            //obtenemos los results, esta es la info de la ciudad
            jso = Methods.JsonToSubJSON(jso, "Results");
            JsonCountry = jso;

            TextView countryNameText = (TextView) findViewById(R.id.countryname);
            TextView precisionText = (TextView) findViewById(R.id.precision);

            countryNameText.setText(Methods.JsonToString(jso, "Name", element.getAlpha2code()));
            precisionText.setText(String.format("Confianza: %.2f%%", element.getPrecision()));

            ImageView imgflag = (ImageView) this.findViewById(R.id.imgflag);

            Picasso.get().load("http://www.geognos.com/api/en/countries/flag/" + element.getAlpha2code() + ".png")
                    .into(imgflag);
        }
        MyLogs.info("-------------------------\n nuevo viaje: " + (JsonCountry != null));
        MyLogs.info(JsonCountry.toString());
    }



    private void strartMap(JsonObject countryObj) {
        Intent intent = new Intent(MainActivity.this, MapViewLayout.class);
        Bundle b = new Bundle();
        b.putString("countryData", countryObj.toString());
        intent.putExtras(b);
        startActivity(intent);
    }

}