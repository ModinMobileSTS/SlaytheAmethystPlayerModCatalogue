# Catalogue regression fixtures

`catalogue-example/` contains the metadata, Java 8 JAR, manifests and source
previously used by the published Catalogue Example entry. Tests read these files
directly and copy them into temporary catalogues as needed.

Keep fixtures outside `mods/`: removing or updating a community entry must not
break regression tests, and test-only examples must not appear in the generated
community index or README. The JAR is inspected as ZIP data, never executed.
