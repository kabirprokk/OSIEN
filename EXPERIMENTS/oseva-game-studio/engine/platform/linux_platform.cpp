// OGS Engine | version 0.1.0 | Linux platform placeholder
#if defined(__linux__)

#include "platform/platform.h"

namespace ogs {

class LinuxPlatform final : public Platform {
public:
    bool initialize(const ApplicationConfig&) override { return false; }
    void pump_events(Application&) override {}
    void fixed_update(float) override {}
    void present(Application&) override {}
    void shutdown() noexcept override {}
};

Platform* create_linux_platform(Application&) {
    return new LinuxPlatform();
}

} // namespace ogs

#endif

