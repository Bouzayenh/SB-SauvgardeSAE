package sae.sauvgarde.server;

import sae.sauvgarde.common.FileBackup;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLSocket;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class ClientHandler extends Thread {
    private SSLSocket sslSocket;

    public ClientHandler(SSLSocket socket) {
        this.sslSocket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(sslSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(sslSocket.getOutputStream())) {

            String username = (String) in.readObject();

            Object obj;
            while ((obj = in.readObject()) != null) {
                String userBackupDir = Server.getBackupDirForUser(username);
                String userZipBackupDir = Server.getZipBackupDirForUser(username);
                if (obj instanceof FileBackup) {
                    FileBackup backup = (FileBackup) obj;
                    if (backup.getFileName().endsWith(".zip")) {
                        handleZipBackup(backup, out, userZipBackupDir);
                    } else {
                        saveBackup(backup, out, userBackupDir);
                    }
                } else if (obj instanceof String) {
                    String command = (String) obj;
                    if (command.startsWith("RESTORE:")) {
                        String baseFolderName = command.substring(8);
                        restoreFiles(baseFolderName, out, userBackupDir);
                    } else if (command.equals("ZIP_RESTORE_REQUEST")) {
                        sendZipFileList(out, userZipBackupDir);
                    } else if (command.startsWith("ZIP_RESTORE:")) {
                        String zipFileName = command.substring(12);
                        handleZipRestore(zipFileName, out, userZipBackupDir);
                    }
                }
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                sslSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveBackup(FileBackup backup, ObjectOutputStream out , String Path) throws IOException {
        try {
            String relativePath = backup.getFileName();
            Path backupPath = Paths.get(Path, relativePath);

            // Ensure the path is still within the backup directory
            if (!backupPath.startsWith(Paths.get(Server.BACKUP_DIR))) {
                out.writeObject("Invalid file path: " + relativePath);
                return;
            }

            // Create directories if they don't exist
            if (Files.notExists(backupPath.getParent())) {
                Files.createDirectories(backupPath.getParent());
            }

            byte[] fileContent = Base64.getDecoder().decode(backup.getFileContent());
            SecretKey secretKey = ConfigLoader.getSecretKey(); // Load the secret key
            byte[] encryptedContent = encrypt(fileContent, secretKey); // Encrypt content

            Files.write(backupPath, encryptedContent);
            out.writeObject("Backup updated for: " + backup.getFileName());
        } catch (Exception e) {
            e.printStackTrace();
            out.writeObject("Error processing file: " + backup.getFileName());
        }
        out.flush();
    }

    private void restoreFiles(String baseFolderName, ObjectOutputStream out, String path) throws IOException {
        Path backupFolderPath = Paths.get(path, baseFolderName);
        if (Files.notExists(backupFolderPath)) {
            out.writeObject("No backup found for the specified folder.");
            return;
        }

        try (Stream<Path> paths = Files.walk(backupFolderPath)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    byte[] fileContent = Files.readAllBytes(file);
                    SecretKey secretKey = ConfigLoader.getSecretKey(); // Load the secret key
                    byte[] decryptedContent = decrypt(fileContent, secretKey); // Decrypt content
                    String encodedContent = Base64.getEncoder().encodeToString(decryptedContent);
                    String relativePath = backupFolderPath.relativize(file).toString();
                    out.writeObject(new FileBackup(relativePath, encodedContent));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        out.writeObject(null); // Indicate end of file transmission
    }


    private void handleZipBackup(FileBackup backup, ObjectOutputStream out , String zipBackupPath) throws IOException {
        try {
            // Ensure user-specific zip backup directory exists
            Path userZipBackupDir = Paths.get(zipBackupPath);
            if (Files.notExists(userZipBackupDir)) {
                Files.createDirectories(userZipBackupDir);
            }

            Path zipFilePath = userZipBackupDir.resolve(backup.getFileName());

            byte[] zipData = Base64.getDecoder().decode(backup.getFileContent());
            SecretKey secretKey = ConfigLoader.getSecretKey(); // Make sure this method exists and provides the necessary key
            byte[] encryptedZipData = encrypt(zipData, secretKey);

            Files.write(zipFilePath, encryptedZipData);
            out.writeObject("Zip backup created for: " + backup.getFileName());
        } catch (Exception e) {
            e.printStackTrace();
            out.writeObject("Error processing zip file: " + backup.getFileName());
        }
        out.flush();
    }


    private void sendZipFileList(ObjectOutputStream out, String Path) throws IOException {
        try {
            List<String> zipFiles = Files.walk(Paths.get(Path))
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());

            out.writeObject(zipFiles);
        } catch (Exception e) {
            e.printStackTrace();
            out.writeObject("Error retrieving zip file list.");
        }
        out.flush();
    }

    private void handleZipRestore(String zipFileName, ObjectOutputStream out,String path) throws IOException {
        Path zipFilePath = Paths.get(path, zipFileName);
        if (Files.notExists(zipFilePath)) {
            out.writeObject("No zip backup found with the specified name.");
            return;
        }

        try {
            byte[] encryptedZipData = Files.readAllBytes(zipFilePath);
            SecretKey secretKey = ConfigLoader.getSecretKey(); // Load the secret key
            byte[] decryptedZipData = decrypt(encryptedZipData, secretKey); // Decrypt the zip data

            String encodedZipData = Base64.getEncoder().encodeToString(decryptedZipData);
            out.writeObject(new FileBackup(zipFileName, encodedZipData));
        } catch (Exception e) {
            e.printStackTrace();
            out.writeObject("Error processing zip file: " + zipFileName);
        }
        out.flush();
    }


    // Encryption method pour test
    static byte[] encrypt(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    // Decryption method pour test
    static byte[] decrypt(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }
}