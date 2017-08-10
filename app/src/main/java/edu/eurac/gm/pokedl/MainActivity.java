package edu.eurac.gm.pokedl;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private String SelectedFilePath;
    ArrayAdapter<String> credspinneradapter;
    CredStoreManager csm;

    protected void reloadCreds(){
        credspinneradapter.clear();
        Set<String> credset = csm.getCredentialSet();
        for(String s:credset){
            credspinneradapter.add(s.split("\\|")[0]);
        }
        String defcred = csm.fetchDefaultCredentialName(getApplicationContext());
        if(defcred != null) {
            Spinner spinner = (Spinner) findViewById(R.id.spinner);
            spinner.setSelection(credspinneradapter.getPosition(defcred));
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String serverurl = (String) parent.getItemAtPosition(pos);
        csm.setDefaultCredentials(serverurl);
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        csm = new CredStoreManager(getApplicationContext());
        credspinneradapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        credspinneradapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner sItems = (Spinner) findViewById(R.id.spinner);
        sItems.setAdapter(credspinneradapter);
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);
        reloadCreds();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        160);

            }
        }
    }

    public void addCredentials(View view){
        EditText et1 = (EditText)findViewById(R.id.editText);
        EditText et2 = (EditText)findViewById(R.id.editText2);
        EditText et3 = (EditText)findViewById(R.id.editText3);
        EditText et4 = (EditText)findViewById(R.id.editText4);
        String username = et1.getText().toString();
        String password= et2.getText().toString();
        String serverurl = et3.getText().toString();
        String servername = et4.getText().toString();
        DLManager.startCheckCredentials(servername,serverurl,username,password,this);
    }

    private void clearTextFields(){
        ((EditText)findViewById(R.id.editText)).setText("");
        ((EditText)findViewById(R.id.editText2)).setText("");
        ((EditText)findViewById(R.id.editText3)).setText("");
        ((EditText)findViewById(R.id.editText4)).setText("");
    }

    protected void credentialsVerified(String servername, String url, String username, String password){
        Toast.makeText(getApplicationContext(), "Credentials verified", Toast.LENGTH_LONG).show();
        csm.addCredentials(servername,url,username,password,true,getApplicationContext());
        clearTextFields();
        reloadCreds();
    }

    protected void credentialsInvalid(int Statuscode){
        String hint = "";
        switch(Statuscode){
            case 401:
                hint = "(Password?)";
                break;
            default:
                hint= "(URL?)";
                break;
        }
        Toast.makeText(getApplicationContext(), "Credentials invalid" + hint, Toast.LENGTH_LONG).show();
    }

    public void removeDefaultCredentials(View view){
        csm.removeCredentials(csm.fetchDefaultCredentialName(getApplicationContext()));
        reloadCreds();
    }

    public void removeAllCredentials(View view){
        csm.removeAllCredentials();
        reloadCreds();
    }
}
