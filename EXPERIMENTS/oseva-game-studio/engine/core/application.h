// OGS Engine | version 0.1.0 | Application lifecycle
#pragma once

#include "core/types.h"

namespace ogs {

class Platform;

struct ApplicationConfig {
    const char* title = "OSEVA Game Studio";
    LogicalSize logical_size{};
    bool resizable = true;
};

class Application {
public:
    explicit Application(ApplicationConfig config = {});
    ~Application();

    Application(const Application&) = delete;
    Application& operator=(const Application&) = delete;

    int run();
    void request_quit() noexcept;
    [[nodiscard]] bool is_running() const noexcept;
    [[nodiscard]] const ApplicationConfig& config() const noexcept;

private:
    ApplicationConfig config_{};
    Platform* platform_ = nullptr;
    bool running_ = false;
};

} // namespace ogs

