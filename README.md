# Phoenix Art ASCII Studio — Android

Offline Android WebView aplikacija s originalnim HTML studijem i logotipom. Uvoz datoteka i izvoz PNG/SVG/TXT koriste Androidov sistemski dijalog. Nema INTERNET dozvole.

## APK
Push na main automatski pokreće GitHub Actions. Nakon uspješnog builda preuzmi Phoenix-ASCII-Studio-APK iz Artifacts. Raspakiraj ZIP i instaliraj APK. Workflow se može pokrenuti i ručno.

## Lokalno
JDK 17, Gradle 8.7, Android SDK 34, Build Tools 34.0.0. Pokreni gradle :app:assembleDebug.

Debug potpis služi za testiranje. Za trajne nadogradnje/release treba postojani signing key. Rad na Android uređaju još nije testiran; podrška za medijske formate ovisi o WebViewu.
