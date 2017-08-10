package edu.eurac.gm.pokedl;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

class CredStoreManager {
    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String KEY_ALIAS = "PDLGM17_KS";
    private static final String RSA_MODE =  "RSA/ECB/PKCS1Padding";
    private static final String SHARED_PREFERENCE_NAME = "PDLGM17_SP";
    private static final String AES_MODE = "AES/ECB/PKCS7Padding";
    private KeyStore keyStore;
    private Context context;

    protected CredStoreManager(Context context){

        this.context = context;
        this.generateKeysIfNeeded();
    }

    protected boolean setDefaultCredentials(String servername){
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString("defaultcredentials",servername);
        edit.commit();
        return true;
    }

    protected Set<String> getCredentialSet(){
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        Set<String> credentialset = pref.getStringSet("credentials",null);
        Set<String> newcredentialset;
        if(credentialset != null){
            newcredentialset = new HashSet<String>(credentialset);
        }else{
            newcredentialset = new HashSet<String>();
        }
        return newcredentialset;
    }

    protected String getCredentials(String servername){
        Set<String> credentialset = this.getCredentialSet();
        for(String credential : credentialset){
            String credservername = credential.split("\\|")[0];
            if(servername.equalsIgnoreCase(credservername)){
                return credential;
            }
        }
        return null;
    }

    protected boolean setCredentialSet(Set<String> newset){
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        edit.putStringSet("credentials",newset);
        edit.commit();
        return true;
    }

    protected boolean credentialsPresent(String servername){
        Set<String> credentialset = this.getCredentialSet();
        for(String credential : credentialset){
            String credservername = credential.split("\\|")[0];
            if(servername.equalsIgnoreCase(credservername)){
                return true;
            }
        }
        return false;
    }

    protected boolean removeCredentials(String servername){
        Set<String> credentialset = this.getCredentialSet();
        for(String credential : credentialset){
            String credservername = credential.split("\\|")[0];
            if(servername.equalsIgnoreCase(credservername)){
                credentialset.remove(credential);
                setCredentialSet(credentialset);
                return true;
            }
        }
        return false;
    }

    protected boolean removeAllCredentials(){
        setCredentialSet(new HashSet<String>());
        return true;
    }

    protected boolean addCredentials(String servername, String serverurl, String usr, String pass, boolean replace, Context context){
        if(credentialsPresent(servername)){
            if(replace){
                removeCredentials(servername);
            }else{
                return false;
            }
        }
        try {
            byte[] usrbytes = usr.getBytes("UTF-8");
            byte[] passbytes = pass.getBytes("UTF-8");
            String encusr = this.encrypt(context, usrbytes);
            String encpass = this.encrypt(context, passbytes);
            SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = pref.edit();
            Set<String> credentialset = this.getCredentialSet();
            credentialset.add(servername+"|"+serverurl+"|"+encusr+"|"+encpass);
            edit.putStringSet("credentials",credentialset);
            edit.commit();
        }catch(Exception e){
            return false;
        }
        return true;
    }

/*
    protected boolean storeCredentials(String usr, String pass, Context context){
        generateKeysIfNeeded();
        try {
            //storeAESKEY();
            byte[] usrbytes = usr.getBytes("UTF-8");
            byte[] passbytes = pass.getBytes("UTF-8");
            String encusr = encrypt(context, usrbytes);
            String encpass = encrypt(context, passbytes);
            SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = pref.edit();
            edit.putString("CREDU", encusr);
            edit.putString("CREDP", encpass);
            edit.commit();
            return true;


        }catch(Exception e){
            System.out.println("Exception while storing credentials");
            System.out.println(e.getMessage());
            return false;
        }
    }


    protected boolean clearCredentials(Context context){
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        edit.remove("CREDU");
        edit.remove("CREDP");
        edit.commit();
        return true;
    }
    */

    protected String fetchDefaultCredentialName(Context context){
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        return pref.getString("defaultcredentials",null);
    }

    protected String fetchDefaultUsername(Context context) throws Exception {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String defaultserver = pref.getString("defaultcredentials",null);
        if(defaultserver == null){

        }
        String encusr = getCredentials(defaultserver).split("\\|")[2];
        String decusr = new String(decrypt(context,Base64.decode(encusr,Base64.DEFAULT)));
        return decusr;
    }

    protected String fetchDefaultPassword(Context context) throws Exception {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String defaultserver = pref.getString("defaultcredentials",null);
        if(defaultserver == null){

        }
        String encpass = getCredentials(defaultserver).split("\\|")[3];
        String decpass = new String(decrypt(context,Base64.decode(encpass,Base64.DEFAULT)));
        return decpass;
    }

    protected String fetchDefaultServerurl(Context context) throws Exception {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String defaultserver = pref.getString("defaultcredentials",null);
        if(defaultserver == null){

        }
        String serverurl = getCredentials(defaultserver).split("\\|")[1];
        return serverurl;
    }


    protected String fetchStoredUsername(Context context) throws Exception {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String encusr = pref.getString("CREDU",null);
        String decusr = new String(decrypt(context,Base64.decode(encusr,Base64.DEFAULT)));
        return decusr;
    }

    protected String fetchStoredPassword(Context context) throws Exception {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String encpass = pref.getString("CREDP",null);
        String decpass = new String(decrypt(context,Base64.decode(encpass,Base64.DEFAULT)));
        return decpass;
    }

    private void generateKeysIfNeeded() {
        try {
            keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);


// Generate the RSA key pairs
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                // Generate a key pair for encryption
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 1);
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(this.context)
                        .setAlias(KEY_ALIAS)
                        .setSubject(new X500Principal("CN=" + KEY_ALIAS))
                        .setSerialNumber(BigInteger.TEN)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, AndroidKeyStore);
                kpg.initialize(spec);
                kpg.generateKeyPair();
                storeAESKEY();
            }
        } catch (Exception e) {
        }
    }


    private byte[] rsaEncrypt(byte[] secret) throws Exception{
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
        Cipher inputCipher = Cipher.getInstance(RSA_MODE);
        inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, inputCipher);
        cipherOutputStream.write(secret);
        cipherOutputStream.close();

        byte[] vals = outputStream.toByteArray();
        return vals;
    }

    private  byte[]  rsaDecrypt(byte[] encrypted) throws Exception {
        keyStore = KeyStore.getInstance(AndroidKeyStore);
        keyStore.load(null);
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(KEY_ALIAS, null);
        Cipher output = Cipher.getInstance(RSA_MODE);
        output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
        CipherInputStream cipherInputStream = new CipherInputStream(
                new ByteArrayInputStream(encrypted), output);
        ArrayList<Byte> values = new ArrayList<>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte)nextByte);
        }

        byte[] bytes = new byte[values.size()];
        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i).byteValue();
        }
        return bytes;
    }

    private void storeAESKEY() throws Exception {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String encryptedKeyB64 = pref.getString("CREDKEY", null);
        if (encryptedKeyB64 == null) {
            byte[] key = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(key);
            byte[] encryptedKey = rsaEncrypt(key);
            encryptedKeyB64 = Base64.encodeToString(encryptedKey, Base64.DEFAULT);
            SharedPreferences.Editor edit = pref.edit();
            edit.putString("CREDKEY", encryptedKeyB64);
            edit.commit();
        }
    }


    private Key getSecretKey(Context context) throws Exception{
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String encryptedKeyB64 = pref.getString("CREDKEY", null);
        byte[] encryptedKey = Base64.decode(encryptedKeyB64, Base64.DEFAULT);
        byte[] key = rsaDecrypt(encryptedKey);
        return new SecretKeySpec(key, "AES");
    }

    private String encrypt(Context context, byte[] input) throws Exception {
        Cipher c = Cipher.getInstance(AES_MODE, "BC");
        c.init(Cipher.ENCRYPT_MODE, getSecretKey(context));
        byte[] encodedBytes = c.doFinal(input);
        String encryptedBase64Encoded =  Base64.encodeToString(encodedBytes, Base64.DEFAULT);
        return encryptedBase64Encoded;
    }


    private byte[] decrypt(Context context, byte[] encrypted) throws Exception {
        Cipher c = Cipher.getInstance(AES_MODE, "BC");
        c.init(Cipher.DECRYPT_MODE, getSecretKey(context));
        byte[] decodedBytes = c.doFinal(encrypted);
        return decodedBytes;
    }
}
