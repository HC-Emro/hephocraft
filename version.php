<?php
// Créez un tableau associatif avec les données que vous souhaitez renvoyer en JSON
$data = array(
    "Launcher" => "7",
    "Version MC" => "1.20.2",
    "resources_url" => "https://www.dl.dropboxusercontent.com/scl/fi/ne17ph4ugepzgjlo6ig1q/installeur.zip?rlkey=qezfsx707sqzmv1nuxgnzqemy&dl=0",
    "image_url" => "https://i.ibb.co/s5m4pYL/installeur.png"
);

// Définissez l'en-tête de réponse pour indiquer que le contenu est au format JSON
header('Content-Type: application/json');

// Utilisez la fonction json_encode pour convertir le tableau associatif en JSON
echo json_encode($data);
?>
