// OGS Engine | version 0.1.0 | Platform factory
#include "platform/platform.h"

namespace ogs {

#if defined(_WIN32)
Platform* create_windows_platform(Application& application);
#elif defined(__linux__)
Platform* create_linux_platform(Application& application);
#endif

Platform* create_platform(Application& application) {
#if defined(_WIN32)
    return create_windows_platform(application);
#elif defined(__linux__)
    return create_linux_platform(application);
#else
    (void)application;
    return nullptr;
#endif
}

} // namespace ogs

