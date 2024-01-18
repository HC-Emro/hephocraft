import javax.swing.*;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

public class InstallateurFenetre extends JFrame {

    private JTextField cheminTextField;
    private JLabel imageLabel;
    private JButton installerButton;
    private String launcherVersion = "7";
    private String versionFabric = "fabric-loader-0.15.3-1.20.2";
    boolean erreurProduite = false;

    // La méthode pour modifier le fichier .txt
    private void modifierLigneDansFichier(String cheminFichier, String ancienneValeur, String nouvelleValeur) {
        try {
            cheminFichier = cheminFichier + File.separator + "options.txt";
            Path fichier = Paths.get(cheminFichier);

            // Lecture de toutes les lignes du fichier dans une liste
            List<String> lignes = Files.readAllLines(fichier, StandardCharsets.UTF_8);

            // Indique si l'ancienne valeur a été trouvée
            boolean ancienneValeurTrouvee = false;

            // Parcourir la liste et trouver la ligne à modifier
            for (int i = 0; i < lignes.size(); i++) {
                String ligne = lignes.get(i);
                if (ligne.contains(ancienneValeur)) {

                    // Modifier la ligne si l'ancienne valeur est trouvée
                    lignes.set(i, nouvelleValeur);

                    ancienneValeurTrouvee = true;
                    break;  // Arrêter la recherche après avoir trouvé la première occurrence
                }
            }


            // Si l'ancienne valeur n'est pas trouvée, ajouter une nouvelle ligne
            if (!ancienneValeurTrouvee) {
                lignes.add(nouvelleValeur);
                System.out.println("Nouvelle ligne ajoutée : " + nouvelleValeur);
            }

            // Écriture des lignes modifiées dans le fichier
            Files.write(fichier, lignes, StandardCharsets.UTF_8);

            System.out.println("Le fichier a été modifié avec succès.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date currentDate = new Date();
        return dateFormat.format(currentDate);
    }
    // Modifier le launcher_profiles.json pour insérer le profil HC
    private void modifierLauncherProfiles(String cheminfichier) {
        cheminfichier = cheminfichier + File.separator + "launcher_profiles.json";

        try {
            // Étape 1 : Lire le fichier JSON existant et le convertir en un objet JsonObject
            String jsonString = new String(Files.readAllBytes(Paths.get(cheminfichier)));
            JSONObject launcherProfiles = new JSONObject(jsonString);

            // Extraire l'objet JSON "profiles"
            if (launcherProfiles.has("profiles")) {
                JSONObject profiles = launcherProfiles.getJSONObject("profiles");

                // Créez une copie de la liste des clés (noms de profils)
                List<String> profileKeys = new ArrayList<>();
                Iterator<String> keysIterator = profiles.keys();
                while (keysIterator.hasNext()) {
                    String key = keysIterator.next();
                    profileKeys.add(key);
                }

                // Parcourez la liste des clés et supprimez les profils "HephoCraft" existants
                for (String key : profileKeys) {
                    if (key.contains("HephoCraft")) {
                        profiles.remove(key);
                    }
                }

                // Créez un nouvel objet JSON pour le profil HephoCraft
                JSONObject hephoCraftProfile = new JSONObject();
                hephoCraftProfile.put("created", getCurrentDateTime());
                hephoCraftProfile.put("lastUsed", getCurrentDateTime());
                hephoCraftProfile.put("lastVersionId", versionFabric);
                hephoCraftProfile.put("name", "HephoCraft");
                hephoCraftProfile.put("type", "custom");

                // Ajoutez le profil HephoCraft à la section "profiles"
                profiles.put("HephoCraft", hephoCraftProfile);
            }

            // Étape 3 : Écrire le fichier JSON mis à jour
            Files.write(Paths.get(cheminfichier), launcherProfiles.toString(4).getBytes());
            System.out.println("Contenu du fichier launcher_profiles.json modifié avec succès.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
                                                "Veuillez lancer au moins une fois votre jeu en Vanilla " + mcVersion,
                                                "Erreur", JOptionPane.ERROR_MESSAGE);
                                    }
                                });
                            } else {
                                String modsDirectory = selectedDirectory + File.separator + "mods";
                                supprimerDossier(modsDirectory);

                                // Ajoute la logique pour modifier le fichier .txt ici
                                modifierLigneDansFichier(selectedDirectory, "forceUnicodeFont:", "forceUnicodeFont:false");
                                modifierLigneDansFichier(selectedDirectory, "lang:", "lang:fr_fr");
                                modifierLigneDansFichier(selectedDirectory, "tutorialStep:", "tutorialStep:none");
                                modifierLigneDansFichier(selectedDirectory, "skipMultiplayerWarning:", "skipMultiplayerWarning:true");
                                modifierLigneDansFichier(selectedDirectory, "joinedFirstServer:", "joinedFirstServer:true");
                                modifierLigneDansFichier(selectedDirectory, "onboardAccessibility:", "onboardAccessibility:false");
                                modifierLigneDansFichier(selectedDirectory, "soundCategory_music:", "soundCategory_music:0.0");
                                modifierLigneDansFichier(selectedDirectory, "key_gui.xaero_open_map:", "key_gui.xaero_open_map:key.keyboard.semicolon");

                                String resourcesUrl = getResourcesUrlFromApi();

                                if (resourcesUrl != null) {
                                    telechargerEtExtraireZIP(resourcesUrl, selectedDirectory);
                                    if (erreurProduite == false) {
                                        modifierLauncherProfiles(selectedDirectory);
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
            URI uri = new URI("https://team-hc.com/_functions/version");
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
            URI uri = new URI("https://team-hc.com/_functions/version");
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
            URI uri = new URI("https://team-hc.com/_functions/version");
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
            URI uri = new URI("https://team-hc.com/_functions/version");
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
