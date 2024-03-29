package sae.sauvgarde.client;

import org.apache.catalina.User;
import sae.sauvgarde.common.FileBackup;
import sae.sauvgarde.common.ZipUtility;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final String PARAMETER_FILE_NAME = "C:\\Users\\bh13h\\IdeaProjects\\SauvgardeJ\\src\\main\\java\\client\\parameters.txt";

    private static void backupFiles(Path folderPath, Set<String> allowedExtensions, ObjectOutputStream out, String baseFolderName, ObjectInputStream in) throws IOException {
        try (Stream<Path> paths = Files.walk(folderPath)) {
            paths.forEach(path -> {
                if (Files.isDirectory(path)) {
                    return; // Skip directories themselves, but still process their contents
                }

                if (!hasAllowedExtension(path, allowedExtensions)) {
                    return; // Skip files that do not have allowed extensions
                }

                try {
                    byte[] fileContent = Files.readAllBytes(path);
                    String encodedContent = Base64.getEncoder().encodeToString(fileContent);
                    // Combine base folder name with relative path
                    String relativePath = baseFolderName + File.separator + folderPath.relativize(path).toString();
                    out.writeObject(new FileBackup(relativePath, encodedContent));
                    String confirmation = (String) in.readObject();
                    System.out.println(confirmation);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    private static void restoreFiles(Path folderPath, ObjectOutputStream out, ObjectInputStream in, String baseFolderName) throws IOException, NoSuchAlgorithmException {
        out.writeObject("RESTORE:" + baseFolderName);
        out.flush();

        while (true) {
            try {
                Object response = in.readObject();
                if (response == null || !(response instanceof FileBackup)) {
                    break; // End of file transmission
                }

                FileBackup fileBackup = (FileBackup) response;
                Path filePath = folderPath.resolve(fileBackup.getFileName());
                byte[] fileContent = Base64.getDecoder().decode(fileBackup.getFileContent());

                if (!isFileChanged(filePath, fileContent)) {
                    System.out.println("File is the same, no need to restore: " + filePath);
                    continue;
                }

                Files.createDirectories(filePath.getParent());
                Files.write(filePath, fileContent);
                System.out.println("Restored: " + filePath);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private static void backupWithZip(Path folderPath, ObjectOutputStream out, String baseFolderName, ObjectInputStream in) throws IOException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String zipFileName = baseFolderName + "_" + timestamp + ".zip";
        Path zipFilePath = Paths.get(folderPath.getParent().toString(), zipFileName);

        ZipUtility.zipFolder(folderPath, zipFilePath);

        // Send zip file to server
        byte[] zipData = Files.readAllBytes(zipFilePath);
        String encodedZipData = Base64.getEncoder().encodeToString(zipData);
        out.writeObject(new FileBackup(zipFileName, encodedZipData));
        out.flush();

        try {
            String confirmation = (String) in.readObject();
            System.out.println(confirmation);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void restoreWithZip(Path folderPath, ObjectOutputStream out, ObjectInputStream in) throws IOException {
        out.writeObject("ZIP_RESTORE_REQUEST");
        out.flush();

        try {
            Object response = in.readObject();
            if (response instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> zipFiles = (List<String>) response;
                for (int i = 0; i < zipFiles.size(); i++) {
                    System.out.println((i + 1) + ". " + zipFiles.get(i));
                }

                System.out.print("Enter the number of the zip to restore: ");
                int choice = new Scanner(System.in).nextInt();
                if (choice < 1 || choice > zipFiles.size()) {
                    System.out.println("Invalid choice.");
                    return;
                }

                out.writeObject("ZIP_RESTORE:" + zipFiles.get(choice - 1));
                out.flush();

                // Receive the zip file
                FileBackup fileBackup = (FileBackup) in.readObject();
                if (fileBackup != null) {
                    byte[] zipData = Base64.getDecoder().decode(fileBackup.getFileContent());
                    Path zipFilePath = folderPath.resolve(fileBackup.getFileName());
                    Files.write(zipFilePath, zipData);

                    // Unzip the file
                    ZipUtility.unzip(zipFilePath, folderPath);
                    System.out.println("Restored: " + zipFilePath);
                } else {
                    System.out.println("No file received for restoration.");
                }
            } else {
                System.out.println("No zip files available for restore.");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private static boolean isFileChanged(Path path, byte[] newContent) throws IOException, NoSuchAlgorithmException {
        if (!Files.exists(path)) {
            return true; // File doesn't exist, so it's changed/new
        }

        // Use checksum comparison for files larger than 50 MB
        final long fileSizeThreshold = 50 * 1024 * 1024; // 50 MB in bytes
        long existingFileSize = Files.size(path);

        if (existingFileSize > fileSizeThreshold) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] existingContent = Files.readAllBytes(path);
            byte[] existingChecksum = digest.digest(existingContent);
            byte[] newChecksum = digest.digest(newContent);

            return !Arrays.equals(existingChecksum, newChecksum);
        } else {
            // For smaller files, use direct byte-by-byte comparison
            byte[] existingContent = Files.readAllBytes(path);
            return !Arrays.equals(existingContent, newContent);
        }
    }



//    private static Set<String> loadAllowedExtensions() {
//        Set<String> extensions = new HashSet<>();
//        try (BufferedReader reader = new BufferedReader(new FileReader(PARAMETER_FILE_NAME))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                extensions.add(line.trim());
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return extensions;
//    }

    private static boolean hasAllowedExtension(Path path, Set<String> allowedExtensions) {
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            return true;
        }
        String fileName = path.getFileName().toString();
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            String extension = fileName.substring(i + 1);
            return allowedExtensions.contains(extension);
        }
        return false;
    }

    private static SSLSocket createSSLSocket() throws Exception {
        // Load the truststore
        URL truststoreResource = Client.class.getClassLoader().getResource("SSL/client.truststore.jks");
        if (truststoreResource == null) {
            throw new FileNotFoundException("Le fichier 'truststore.jks' est introuvable.");
        }
        String truststorePassword = "furryfurry";

        KeyStore ts = KeyStore.getInstance("JKS");
        ts.load(truststoreResource.openStream(), truststorePassword.toCharArray());

        // Initialize the TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        // Initialize SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        // Create and return SSLSocket
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        return (SSLSocket) sslSocketFactory.createSocket(SERVER_ADDRESS, SERVER_PORT);
    }

    public static String performOperation(String folderPathStr, String operation, String useZipStr,String username,Set<String> allowedExtensions) throws Exception {
        SSLSocket sslSocket = createSSLSocket();
        ObjectOutputStream out = new ObjectOutputStream(sslSocket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(sslSocket.getInputStream());

        out.writeObject(username);
        out.flush();

        try {
            Path folderPath = Paths.get(folderPathStr);
            if (!Files.isDirectory(folderPath)) {
                throw new InvalidPathException(folderPathStr, "The specified path is not a directory.");
            }

            String baseFolderName = folderPath.getFileName().toString();

            boolean useZip = "yes".equalsIgnoreCase(useZipStr);
            if (useZip) {
                if ("backup".equalsIgnoreCase(operation)) {
                    backupWithZip(folderPath, out, baseFolderName, in);
                } else if ("restore".equalsIgnoreCase(operation)) {
                    restoreWithZip(folderPath, out, in);
                }
            } else {
                if ("backup".equalsIgnoreCase(operation)) {
                    backupFiles(folderPath, allowedExtensions, out, baseFolderName, in);
                } else if ("restore".equalsIgnoreCase(operation)) {
                    restoreFiles(folderPath, out, in, baseFolderName);
                }
            }

            return "Operation completed successfully.";
        } catch (Exception e) {
            return "An error occurred: " + e.getMessage();
        } finally {
            out.close();
            sslSocket.close();
        }
    }
}