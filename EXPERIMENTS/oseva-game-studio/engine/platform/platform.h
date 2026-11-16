// OGS Engine | version 0.1.0 | Platform abstraction
#pragma once

#include "core/application.h"

namespace ogs {

class Platform {
public:
    virtual ~Platform() = default;
    virtual bool initialize(const ApplicationConfig& config) = 0;
    virtual void pump_events(Application& application) = 0;
    virtual void fixed_update(float fixed_delta_seconds) = 0;
    virtual void present(Application& application) = 0;
    virtual void shutdown() noexcept = 0;
};

Platform* create_platform(Application& application);

} // namespace ogs

