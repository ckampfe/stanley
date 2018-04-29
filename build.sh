boot build
native-image \
  -H:+ReportUnsupportedElementsAtRuntime \
  -cp .:target \
  stanley.core

