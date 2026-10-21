# Vendored AndroidX components

The nine JARs listed in ../media3-dependencies.json are unmodified classes.jar entries extracted from pinned upstream AARs. Original coordinates, AAR hashes and extracted JAR hashes are retained in that manifest. Combined size: 3,594,046 bytes.

Default builds validate local files and never download them. Restore missing/corrupt files from git; maintainers can explicitly run `python3 cinema-ui/fetch_media3.py --download` to repair them from the recorded upstream URLs.

AndroidX Media3 and ExifInterface are licensed under Apache License 2.0. See ../runtime-assets/Media3-Apache-2.0.txt (also included in the APK). The JARs remain unmodified; the build excludes an unused download notification class when constructing its merged runtime JAR, as documented in build_fusion_runtime.py.
