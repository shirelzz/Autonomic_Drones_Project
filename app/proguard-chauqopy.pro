# Preserve Chaquopy's Python-related classes and resources
-keep class **$py { *; }

# Add specific rules for the classes reported in the errors
-keep class csv$py { *; }
-keep class email.mime.audio$py { *; }
-keep class email.charset$py { *; }
-keep class Lib.csv$py { *; }
-keep class Lib.email.charset$py { *; }
-keep class unittest.__main__$py { *; }

#/* Add similar rules for any other classes reported in the errors */
-keep class Lib.modjy.modjy_response$py { *; }
-keep class encodings.utf_16_le$py { *; }
-keep class distutils.tests.test_versionpredicate$py { *; }
-keep class json.tests.test_float$py { *; }
-keep class distutils.tests.test_install_lib$py { *; }
-keep class Lib.email.utils$py { *; }
-keep class Lib._google_ipaddr_r234$py { *; }
-keep class Lib.distutils.tests.test_install_lib$py { *; }
-keep class Lib.distutils.spawn$py { *; }
-keep class Lib.distutils.tests.test_config$py { *; }
-keep class Lib.lib2to3.fixes.fix_numliterals$py { *; }
-keep class Lib.modjy.modjy_params$py { *; }
-keep class Lib.inspect$py { *; }
-keep class Lib.json.tests.test_default$py { *; }
-keep class Lib.tempfile$py { *; }
-keep class Lib.encodings.shift_jis$py { *; }
-keep class Lib.lib2to3.fixes.fix_long$py { *; }
