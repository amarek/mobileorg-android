package com.matburt.mobileorg;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import java.util.Iterator;
import java.security.SignatureException;
import android.util.Log;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPObjectFactory;
import org.bouncycastle2.openpgp.PGPCompressedData;
import org.bouncycastle2.openpgp.PGPLiteralData;
import org.bouncycastle2.openpgp.PGPPublicKey;
import org.bouncycastle2.openpgp.PGPPublicKeyRing;
import org.bouncycastle2.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle2.openpgp.PGPSecretKey;
import org.bouncycastle2.openpgp.PGPPrivateKey;
import org.bouncycastle2.openpgp.PGPSecretKeyRing;
import org.bouncycastle2.openpgp.PGPUtil;
import org.bouncycastle2.openpgp.PGPEncryptedDataList;
import org.bouncycastle2.openpgp.PGPEncryptedData;
import org.bouncycastle2.openpgp.PGPPBEEncryptedData;
import org.bouncycastle2.openpgp.PGPOnePassSignature;
import org.bouncycastle2.openpgp.PGPOnePassSignatureList;
import org.bouncycastle2.openpgp.PGPSignature;
import org.bouncycastle2.openpgp.PGPSignatureList;

import org.bouncycastle2.jce.provider.BouncyCastleProvider;

class Encryption
{
    private static final String LT = "MobileOrg";
    static PGPPublicKeyRing publicKeyRing = null;
    static PGPSecretKeyRing secretKeyRing = null;
    public static String passPhrase = null;


    public static void importKeyRings(String filename)
            throws FileNotFoundException, PGPException, IOException 
    {
        Log.i(LT, "Importing secret keyring from " + filename);

        PGPObjectFactory objectFactory = null;

        FileInputStream fileIn = new FileInputStream(filename);
        InputStream in = PGPUtil.getDecoderStream(fileIn);
        objectFactory = new PGPObjectFactory(in);

        Vector<Object> objects = new Vector<Object>();
        Object obj = objectFactory.nextObject();
        while (obj != null) 
        {
            objects.add(obj);
            obj = objectFactory.nextObject();
        }

        for (int i = 0; i < objects.size(); ++i) {
            obj = objects.get(i);


            if (!(obj instanceof PGPSecretKeyRing)) 
            {
                continue;
            }
            secretKeyRing = (PGPSecretKeyRing) obj;
            Log.i(LT, "Secret keyring imported");
        }

    }


    public static PGPSecretKey findSecretKey(long keyId) {
        PGPSecretKey key = secretKeyRing.getSecretKey(keyId);
        if (key != null) 
        {
            return key;
        }
        return null;
    }

    public static PGPPublicKey findPublicKey(long keyId) {
        PGPPublicKey key = null;
        try {
            key = publicKeyRing.getPublicKey(keyId);
            if (key != null) {
                return key;
            }
        } catch (PGPException e) {
            // just not found, can ignore this
        }
        return null;
    }

    public static void askForPassPhrase(Activity act)
    {
        act.showDialog(0);
        //        Dialog pass = passPhraseDialog(act);
        //        pass.show();
    }

    public static Dialog passPhraseDialog(Activity context)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setMessage("Enter passphrase");
        LayoutInflater inflater =
            (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.pass_phrase, null);
        final EditText input = (EditText) view.findViewById(R.id.passPhrase);

        alert.setView(view);

        final Activity activity = context;
        alert.setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Encryption.passPhrase = "" + input.getText();
                                        activity.removeDialog(0);
                                    }
                                });

        alert.setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        activity.removeDialog(0);
                                    }
                                });

        return alert.create();        
    }

    public static boolean decrypt(InputStream inStream, 
                                  OutputStream outStream,
                                  String passPhrase, boolean assumeSymmetric)
        throws IOException, Exception, PGPException, SignatureException 
    {
        InputStream in = PGPUtil.getDecoderStream(inStream);
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();
        long signatureKeyId = 0;


        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        if (enc == null) {
            throw new Exception("Invalid data");
        }

        InputStream clear = null;
        PGPEncryptedData encryptedData = null;


        if (assumeSymmetric) {
            PGPPBEEncryptedData pbe = null;
            Iterator it = enc.getEncryptedDataObjects();
            // find secret key
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof PGPPBEEncryptedData) {
                    pbe = (PGPPBEEncryptedData) obj;
                    break;
                }
            }

            if (pbe == null) {
                throw new Exception("No Symmetric Encryption Packet");
            }

            clear = pbe.getDataStream(passPhrase.toCharArray(), new BouncyCastleProvider());
            encryptedData = pbe;
        } else {
            PGPPublicKeyEncryptedData pbe = null;
            PGPSecretKey secretKey = null;
            Iterator it = enc.getEncryptedDataObjects();
            // find secret key
            while (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof PGPPublicKeyEncryptedData) {
                    PGPPublicKeyEncryptedData encData = (PGPPublicKeyEncryptedData) obj;
                    secretKey = findSecretKey(encData.getKeyID());
                    if (secretKey != null) {
                        pbe = encData;
                        break;
                    }
                }
            }

            if (secretKey == null) {
                throw new Exception("No Secret Key Found");
            }

            PGPPrivateKey privateKey = null;
            try {
                privateKey = secretKey.extractPrivateKey(passPhrase.toCharArray(),
                                                         new BouncyCastleProvider());
            } catch (PGPException e) {
                throw new PGPException("Wrong Pass Phrase");
            }
            clear = pbe.getDataStream(privateKey, new BouncyCastleProvider());
            encryptedData = pbe;
        }

        PGPObjectFactory plainFact = new PGPObjectFactory(clear);
        Object dataChunk = plainFact.nextObject();
        PGPOnePassSignature signature = null;
        PGPPublicKey signatureKey = null;
        int signatureIndex = -1;

        if (dataChunk instanceof PGPCompressedData) {
            PGPObjectFactory fact =
                    new PGPObjectFactory(((PGPCompressedData) dataChunk).getDataStream());
            dataChunk = fact.nextObject();
            plainFact = fact;
        }

        if (dataChunk instanceof PGPOnePassSignatureList) {
            PGPOnePassSignatureList sigList = (PGPOnePassSignatureList) dataChunk;
            for (int i = 0; i < sigList.size(); ++i) {
                signature = sigList.get(i);
                signatureKey = findPublicKey(signature.getKeyID());
                if (signatureKeyId == 0) {
                    signatureKeyId = signature.getKeyID();
                }
                if (signatureKey == null) {
                    signature = null;
                } else {
                    signatureIndex = i;
                    signatureKeyId = signature.getKeyID();
                    break;
                }
            }

            if (signature != null) {
                signature.initVerify(signatureKey, new BouncyCastleProvider());
            } 

            dataChunk = plainFact.nextObject();
        }

        if (dataChunk instanceof PGPLiteralData) {
            PGPLiteralData literalData = (PGPLiteralData) dataChunk;
            OutputStream out = outStream;

            byte[] buffer = new byte[1 << 16];
            InputStream dataIn = literalData.getInputStream();

            int n = 0;
            int done = 0;
            while ((n = dataIn.read(buffer)) > 0) {
                out.write(buffer, 0, n);
                done += n;
                if (signature != null) {
                    try {
                        signature.update(buffer, 0, n);
                    } catch (SignatureException e) {
                        signature = null;
                    }
                }
            }

            if (signature != null) {
                PGPSignatureList signatureList = (PGPSignatureList) plainFact.nextObject();
                PGPSignature messageSignature = (PGPSignature) signatureList.get(signatureIndex);
                if (signature.verify(messageSignature)) {
                    return true;
                } else {
                    return false;
                }
            }
        }


        return true;
    }

}