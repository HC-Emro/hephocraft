import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

public class InstallateurFenetre extends JFrame {

    private JTextField cheminTextField;
    private JLabel imageLabel;
    private JButton installerButton;
    private String launcherVersion = "5";
    boolean erreurProduite = false;
    public InstallateurFenetre() {
        super("Installateur HephoCraft");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(620, 450);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        imageLabel = new JLabel();
        panel.add(imageLabel);

        JLabel label = new JLabel("Choisissez le dossier de votre Minecraft :");
        panel.add(label);

        cheminTextField = new JTextField(50);
        cheminTextField.setMaximumSize(cheminTextField.getPreferredSize());
        panel.add(cheminTextField);

        String defaultDirectory = getDefaultDirectory();
        cheminTextField.setText(defaultDirectory);

        JButton parcourirButton = new JButton("Parcourir");
        parcourirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                choisirDossier();
            }
        });
        panel.add(parcourirButton);

        installerButton = new JButton("Installer");
        installerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                installerButton.setEnabled(false); // Désactiver le bouton
                installerButton.setText("Chargement...");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Vérification de la version du launcher
                        if (!verifierVersionLauncher()) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    installerButton.setText("Installer");
                                    installerButton.setEnabled(true); // Réactiver le bouton
                                }
                            });
                            return;
                        }

                        String selectedDirectory = cheminTextField.getText();
                        String mcVersion = getMcVersionFromApi();

                        if (mcVersion != null) {
                            String versionDirectory = selectedDirectory + File.separator + "versions" + File.separator + mcVersion;

                            if (!verifierPresenceDossier(versionDirectory)) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        installerButton.setText("Installer");
                                        installerButton.setEnabled(true); // Réactiver le bouton
                                        erreurProduite = true;
                                        JOptionPane.showMessageDialog(InstallateurFenetre.this,
                                                "Veuillez lancer au moins une fois votre jeu avec la version : " + mcVersion,
                                                "Erreur", JOptionPane.ERROR_MESSAGE);
                                    }
                                });
                            } else {
                                String modsDirectory = selectedDirectory + File.separator + "mods";
                                supprimerDossier(modsDirectory);

                                String resourcesUrl = getResourcesUrlFromApi();

                                if (resourcesUrl != null) {
                                    telechargerEtExtraireZIP(resourcesUrl, selectedDirectory);
                                    if (erreurProduite == false) {
                                        JOptionPane.showMessageDialog(InstallateurFenetre.this,
                                                "Installation terminée avec succès ! Vous pouvez lancer le jeu avec le profil créé.\nLe nom du profil contient 'HephoCraft' ou 'fabric loader'.", "Installation", JOptionPane.INFORMATION_MESSAGE);
                                    }
                                } else {
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            installerButton.setText("Installer");
                                            installerButton.setEnabled(true); // Réactiver le bouton
                                            erreurProduite = true;
                                            JOptionPane.showMessageDialog(InstallateurFenetre.this,
                                                    "L'URL des ressources n'est pas disponible.", "Erreur", JOptionPane.ERROR_MESSAGE);
                                        }
                                    });
                                }
                            }
                        }

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                installerButton.setText("Installer");
                                installerButton.setEnabled(true); // Réactiver le bouton à la fin du processus
                            }
                        });
                    }
                }).start();
            }
        });
        panel.add(installerButton);

        add(panel);

        afficherImageFromApi();
    }

    private void choisirDossier() {
        JFileChooser fileChooser = new JFileChooser();

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home"), "AppData\\Roaming"));
        } else if (os.contains("mac")) {
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home"), "Library/Application Support"));
        } else {
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        }

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int resultat = fileChooser.showOpenDialog(this);

        if (resultat == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            cheminTextField.setText(selectedFile.getAbsolutePath());
        }
    }

    private boolean verifierVersionLauncher() {
        try {
            URI uri = new URI("https://www.team-hc.com/_functions/version");
            URL url = uri.toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                String response = new Scanner(inputStream).useDelimiter("\\A").next();

                JSONObject json = new JSONObject(response);
                String apiLauncherVersion = json.getString("Launcher");

                if (estVersionLauncherObsolette(apiLauncherVersion)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            erreurProduite = true;
                            JOptionPane.showMessageDialog(InstallateurFenetre.this, "La version du launcher est obsolète. Veuillez le mettre à jour sur https://team-hc.com/jouer.", "Erreur", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                    return false;
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(InstallateurFenetre.this, "La version du launcher est à jour.", "Information", JOptionPane.INFORMATION_MESSAGE);
                        }
                    });
                }
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        erreurProduite = true;
                        JOptionPane.showMessageDialog(InstallateurFenetre.this, "Erreur lors de la vérification de la version du launcher.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }

            connection.disconnect();
        } catch (Exception e) {
            final String errorMessage = e.getMessage(); // Récupère le message d'erreur de l'exception
            final String customMessage = "Erreur lors de la connexion à l'API\n" + errorMessage;

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    erreurProduite = true;
                    JOptionPane.showMessageDialog(
                            InstallateurFenetre.this, customMessage + "\n" + errorMessage, "Erreur", JOptionPane.ERROR_MESSAGE
                    );
                }
            });
        }
        return true;
    }

    private boolean estVersionLauncherObsolette(String version) {
        return !launcherVersion.equals(version);
    }

    private boolean verifierPresenceDossier(String directoryPath) {
        File directory = new File(directoryPath);
        return directory.exists() && directory.isDirectory();
    }

    private void supprimerDossier(String directoryPath) {
        try {
            FileUtils.deleteDirectory(new File(directoryPath));
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    erreurProduite = true;
                    JOptionPane.showMessageDialog(InstallateurFenetre.this, "Erreur lors de la suppression du dossier : " + directoryPath + "\nAssurez-vous d'avoir éteint le jeu au préalable.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private String getMcVersionFromApi() {
        try {
            URI uri = new URI("https://www.team-hc.com/_functions/version");
            URL url = uri.toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                String response = new Scanner(inputStream).useDelimiter("\\A").next();

                JSONObject json = new JSONObject(response);
                return json.getString("Version MC");
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        erreurProduite = true;
                        JOptionPane.showMessageDialog(InstallateurFenetre.this, "Erreur lors de la récupération de la version MC depuis l'API.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    erreurProduite = true;
                    JOptionPane.showMessageDialog(InstallateurFenetre.this, "Erreur lors de la connexion à l'API.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
        return null;
    }

    private String getResourcesUrlFromApi() {
        try {
            URI uri = new URI("https://www.team-hc.com/_functions/version");
            URL url = uri.toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                String response = new Scanner(inputStream).useDelimiter("\\A").next();

                JSONObject json = new JSONObject(response);
                return json.getString("resources_url");
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        erreurProduite = true;
                        JOptionPane.showMessageDialog(InstallateurFenetre.this, "Erreur lors de la récupération de l'URL des ressources.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    erreurProduite = true;
                    JOptionPane.showMessageDialog(InstallateurFenetre.this, "Erreur lors de la connexion à l'API.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
        return null;
    }

    private String getImageUrlFromApi() {
        try {
            URI uri = new URI("https://www.team-hc.com/_functions/version");
            URL url = uri.toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                String response = new Scanner(inputStream).useDelimiter("\\A").next();

                JSONObject json = new JSONObject(response);
                return json.getString("image_url");
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        erreurProduite = true;
                        JOptionPane.showMessageDialog(InstallateurFenetre.this, "Erreur lors de la récupération de l'URL de l'image depuis l'API.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    erreurProduite = true;
                    JOptionPane.showMessageDialog(InstallateurFenetre.this, "Erreur lors de la connexion à l'API.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
        return null;
    }

    private void telechargerEtExtraireZIP(String url, String targetDirectory) {
        try {
            FileUtils.copyURLToFile(new URL(url), new File(targetDirectory + File.separator + "temp.zip"));

            try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(Paths.get(targetDirectory + File.separator + "temp.zip")))) {
                ZipEntry entry = zipIn.getNextEntry();
                while (entry != null) {
                    Path filePath = Paths.get(targetDirectory, entry.getName());
                    if (!entry.isDirectory()) {
                        Files.createDirectories(filePath.getParent());
                        Files.copy(zipIn, filePath, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.createDirectories(filePath);
                    }
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                }
            }

            Files.deleteIfExists(Paths.get(targetDirectory + File.separator + "temp.zip"));
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    erreurProduite = true;
                    JOptionPane.showMessageDialog(InstallateurFenetre.this, "Erreur lors du téléchargement et de l'extraction du fichier ZIP.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private void afficherImageFromApi() {
        String imageUrl = getImageUrlFromApi();
        if (imageUrl != null) {
            try {
                URI uri = new URI(imageUrl);
                URL url = uri.toURL();

                BufferedImage image = ImageIO.read(url);
                ImageIcon imageIcon = new ImageIcon(image.getScaledInstance(600, 300, Image.SCALE_DEFAULT));
                imageLabel.setIcon(imageIcon);
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        erreurProduite = true;
                        JOptionPane.showMessageDialog(InstallateurFenetre.this, "Erreur lors de la lecture de l'image depuis l'URL.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        }
    }

    private String getDefaultDirectory() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return System.getProperty("user.home") + "\\APPDATA\\Roaming\\.minecraft";
        } else if (os.contains("mac")) {
            return System.getProperty("user.home") + "/Library/Application Support/minecraft";
        } else {
            return System.getProperty("user.home") + "/.minecraft";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new InstallateurFenetre().setVisible(true);
            }
        });
    }
}
