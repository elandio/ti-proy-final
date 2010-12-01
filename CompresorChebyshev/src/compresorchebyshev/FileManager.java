/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package compresorchebyshev;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;

/**
 *
 * @author emirhg
 */
public class FileManager {

    private File file;
    private String type;
    private int headerSize;
    private int dataSize;
    private long fileSize;
    private long currentPos;
    private byte[] bytes;
    private byte[] currentDataBlock;
    private int currentBlockSize;
    private int blockSize;
    private long polDegree;
    private long compresionFactor;
    private long scaleFactor;
    private boolean write;

    public long getCompresionFactor() {
        return compresionFactor;
    }

    public void setCompresionFactor(long compresionFactor) {
        this.compresionFactor = compresionFactor;
    }

    public long getDegree() {
        return polDegree;
    }

    public void setDegree(long polDegree) {
        this.polDegree = polDegree;
    }

    public long getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(long scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    /**
     * Creates  new Object FileManager that will operate with the file given in fName
     * @param fName
     * The file path
     */
    /*public FileManager(String fName){
    new FileManager(fName,false);
    }
     */
    public FileManager(String fName, boolean write) {
        file = new File(fName);
        this.write = write;
        if (fName.contains(".")) {

            type = fName.split("\\.")[1];

            try {
                if (!write) {
                    readFile();

                    int i = 0;
                    while (i < bytes.length - 6 && headerSize == 0) {
                        if (bytes[i] == (byte) 'd' && bytes[i + 1] == (byte) 'a' && bytes[i + 2] == (byte) 't' && bytes[i + 3] == (byte) 'a' && bytes[i + 4] == (byte) 0x00 && bytes[i + 5] == 0x40 && bytes[i + 6] == 0x01) {
                            headerSize = i + 7;
                        }
                        i++;
                    }

                    currentPos = headerSize;
                    i = 0;
                    if (type.equals("KL1")) {
                        String compfactor = "", polDe = "", scaleFact = "";
                        while (i < bytes.length && bytes[i] != 13) {
                            compfactor += (char) bytes[i];
                            i++;
                        }
                        i++;
                        compresionFactor = Long.decode(compfactor);

                        while (i < bytes.length && bytes[i] != 13) {
                            polDe += (char) bytes[i];
                            i++;
                        }
                        i++;

                        polDegree = Long.decode(polDe);

                        while (i < bytes.length - 3 && bytes[i] != (byte) 'R') {
                            scaleFact += (char) bytes[i];
                            i++;
                        }
                        i = 0;
                        scaleFactor = Long.decode(scaleFact);

                        blockSize = (int) ((polDegree + 1) * 3);

                        if (file.exists()) {
                            //file.delete();
                        }
                    }
                }

            } catch (IOException e) {
                System.err.println("Ocurrio un error al recuperar los datos del archivo");
                System.err.println(e.toString());
                bytes = null;
            }
        }
    }


    /*
     * Reads the whole file and stores it information in a byte array structure
     */
    private void readFile() throws IOException {
        InputStream is = new FileInputStream(file);

        // Get the size of the file
        fileSize = file.length();

        if (fileSize > Integer.MAX_VALUE) {
            // File is too large
        }

        // Create the byte array to hold the data
        bytes = new byte[(int) fileSize];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        // Close the input stream and return bytes
        is.close();
    }

    /**
     * Gets the whole file in a byte array representation
     * @return
     * An array of bytes that correspond to the file
     */
    public byte[] getBytes() {
        return bytes;
    }

    /**
     * Gets the file header
     * @return
     * The file header
     */
    public byte[] getHeader() {
        byte header[] = new byte[headerSize];
        System.arraycopy(bytes, 0, header, 0, headerSize);
        return header;
    }

    /**
     * Gets the next block of non header data
     * @return
     * A byte array with the next block data, if there is no more blocks available it returns null
     */
    public byte[] getNextDataBlock() {


        if (isNextDataBlock()) {
            //Increments the current position in the data
            if (currentPos < headerSize) {
                currentPos = headerSize;
            } else {
                currentPos += blockSize;
            }

            //Sets the available bytes size to read
            currentBlockSize = blockSize;
            if ((fileSize - currentPos) < blockSize) {
                currentBlockSize = (int) (fileSize - currentPos);
            }
            currentDataBlock = new byte[blockSize];
            System.arraycopy(bytes, (int) currentPos, currentDataBlock, 0, currentBlockSize);
            //Copies an array from the specified source array, beginning at the specified position, to the specified position of the destination array.
        } else {
            return null;
        }
        return currentDataBlock;
    }

    public Coeficiente[] getNextCoeficientesBlock() {
        Coeficiente[] result = new Coeficiente[blockSize / 3];
        byte[] coef = new byte[3];
        getNextDataBlock();

        for (int i = 0; i < currentDataBlock.length; i = i + 3) {
            System.arraycopy(currentDataBlock, i, coef, 0, 2);
            result[i / 3] = new Coeficiente(coef);
        }

        return result;
    }

    /**
     * Verify if there are any available blocks to read
     * @return
     * True if there are available blocks, false otherwise
     */
    public boolean isNextDataBlock() {
        return currentPos < fileSize && blockSize != 0;
    }

    /**
     * Gets the current size of the blocks to read, this parameter should be the
     * the same during all the lecture per block
     * @return
     * The lecture block size in bytes
     */
    public int getBlockSize() {
        return blockSize;
    }

    /**
     * Sets the block size to it's new value
     * @param size
     * New block size
     */
    public void setBlockSize(int size) {
        blockSize = size;
        currentDataBlock = new byte[blockSize];
    }

    public void writeByteArray(String fName, byte[] data) throws IOException {
        OutputStream os = new FileOutputStream(new File(fName));
        os.write(data);
        os.close();
    }

    public long getFileSize() {
        return fileSize;
    }

    public void appendData(byte data) {
        try {
            OutputStream os = new FileOutputStream(file, true);
            os.write(data);
            os.close();
        } catch (IOException e) {
            System.err.println("Couldn't write the data");
        }
    }

    public void appendData(byte[] data) {
        if (write) {
            try {
                OutputStream os = new FileOutputStream(file, true);
                os.write(data);
                os.close();
            } catch (IOException e) {
                System.err.println("Couldn't write the data");
            }
        } else {
            System.err.println("El archivo se abrio en modo lectura");
        }
    }

    public void appendData(String data) {
        if (write) {
            try {
                OutputStream os = new FileOutputStream(file, true);
                DataOutputStream dos = new DataOutputStream(os);
                dos.writeBytes(data);
                os.close();
            } catch (IOException e) {
                System.err.println("Couldn't write the data");
            }
        } else {
            System.err.println("El archivo se abrio en modo lectura");
        }
    }
}