# Web Admin Deployment

## Local (PHP built-in server)
1. From the repo root:
   `php -S localhost:8000 -t web-admin`
2. Open `http://localhost:8000/index.php`

## Google Cloud App Engine (PHP)
1. Ensure you are authenticated:
   `gcloud auth login`
2. Set the project:
   `gcloud config set project YOUR_PROJECT_ID`
3. Deploy from repo root:
   `gcloud app deploy web-admin/app.yaml`
4. Visit the App Engine URL:
   `https://YOUR_PROJECT_ID.appspot.com/index.php`

## Firebase Hosting (Proxy to App Engine)
Firebase Hosting does not run PHP directly, so it proxies requests to App Engine.

1. Initialize Firebase (if not done):
   `firebase init hosting`
2. Ensure `firebase.json` has the correct App Engine service name:
   - For the default service use `"app": "default"`
   - If you deploy a custom service name, update it here.
3. Deploy:
   `firebase deploy --only hosting`

## Notes
- Admin data is stored in Firestore and Auth (Firebase).
- Set these env vars for App Engine:
  - `FIREBASE_PROJECT_ID`
  - `FIREBASE_API_KEY`
  - `FIREBASE_CREDENTIALS` (path to service account JSON on the server)
## Firebase requirements
- Enable **Email/Password** sign-in in Firebase Auth.
- Create a Firebase service account and place it at:
  `web-admin/credentials/firebase-service-account.json`
