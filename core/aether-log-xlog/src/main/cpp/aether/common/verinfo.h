
#ifndef Aether_verinfo_h
#define Aether_verinfo_h

// These will be defined by CMake if not already defined
#ifndef AETHER_REVISION
#define AETHER_REVISION "unknown"
#endif

#ifndef AETHER_PATH
#define AETHER_PATH "aether"
#endif

#ifndef AETHER_URL
#define AETHER_URL ""
#endif

#ifndef AETHER_BUILD_TIME
#define AETHER_BUILD_TIME __DATE__ " " __TIME__
#endif

#ifndef AETHER_TAG
#define AETHER_TAG ""
#endif

#endif
