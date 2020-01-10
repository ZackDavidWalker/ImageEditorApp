package de.hdmstuttgart.bildbearbeiter.utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ImageFileHandler {

    public static final String IMAGE_DIR_LIB = "BBImages";
    public static final String IMAGE_DIR_TMP = "tmp";

    private File imageDir;

    public ImageFileHandler(File applicationDir, String imageSubDir) {
        this.imageDir = new File(applicationDir, imageSubDir);
        if (!imageDir.exists()) imageDir.mkdirs();
    }

    public final boolean saveImage(Bitmap imageToSave, String fileName)
    {
        try {
            File file = new File(imageDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            imageToSave.compress(Bitmap.CompressFormat.PNG, 0, fos);
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public final Bitmap getImage(String imgName) {
        try {
            File imageFile = new File(imageDir, imgName);
            FileInputStream fis = new FileInputStream(imageFile);
            Bitmap decodedBitmap = BitmapFactory.decodeStream(fis);
            fis.close();
            return decodedBitmap;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public File getImageFolder() {
        return imageDir;
    }

    public File createFileWithName(String fileName) {
        return new File(imageDir, fileName);
    }
}