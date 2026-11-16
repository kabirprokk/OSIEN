// OGS Engine | version 0.1.0 | Application lifecycle
#include "core/application.h"

#include "core/log.h"
#include "platform/platform.h"

#include "core/fixed_clock.h"

#include <chrono>

namespace ogs {

Application::Application(const ApplicationConfig config) : config_(config) {}

Application::~Application() {
    delete platform_;
    platform_ = nullptr;
}

int Application::run() {
    platform_ = create_platform(*this);
    if (platform_ == nullptr || !platform_->initialize(config_)) {
        log::error("Platform initialization failed.");
        return 1;
    }

    running_ = true;
    log::info("OGS engine foundation started.");
    FixedClock clock;
    const auto start = std::chrono::steady_clock::now();
    clock.reset(0.0);

    while (running_) {
        platform_->pump_events(*this);
        const auto now = std::chrono::steady_clock::now();
        const double elapsed = std::chrono::duration<double>(now - start).count();
        const std::uint32_t steps = clock.advance(elapsed);
        for (std::uint32_t step = 0; step < steps; ++step) {
            platform_->fixed_update(static_cast<float>(clock.step_seconds()));
        }
        platform_->present(*this);
    }

    platform_->shutdown();
    return 0;
}

void Application::request_quit() noexcept {
    running_ = false;
}

bool Application::is_running() const noexcept {
    return running_;
}

const ApplicationConfig& Application::config() const noexcept {
    return config_;
}

} // namespace ogs

