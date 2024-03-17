# Secure File Backup and Restore System

Système client-serveur en Java utilise le protocole SSL pour une communication sécurisée, offrant une solution fiable pour la sauvegarde et la restauration de fichiers.

## Caractéristiques Principales

- **Communication Sécurisée:** Utilise SSL pour un transfert de données sécurisé entre le client et le serveur.
- **Sérialisation des Objets:** Object Streams pour sérialiser et désérialiser les objets, facilitant la gestion de la sauvegarde des fichiers.
- **Encodage Base64:** Encode le contenu des fichiers assurant une manipulation sûre des données.
- **Interface Graphique:** Utilise Spring Boot pour une gestion intuitive du côté client.
- **Sauvegarde et Restauration:** Opérations simplifiées pour sauvegarder et récupérer des fichiers, y compris le support des fichiers zip.
- **Détection des Modifications de Fichiers:** Assure que les fichiers n'ont pas été altérés avant leur restauration.
- **Chiffrement AES:** Sécurise le contenu des fichiers durant les opérations de sauvegarde et de restauration.
- **Authentification Centralisée:** Intègre Keycloak pour une gestion d'authentification robuste et centralisée.
