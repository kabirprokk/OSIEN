// OGS Engine | version 0.1.0 | Minimal structured logging
#include "core/log.h"

#include <iostream>

namespace ogs::log {

void info(const std::string_view message) noexcept {
    std::clog << "[OGS][INFO] " << message << '\n';
}

void warning(const std::string_view message) noexcept {
    std::clog << "[OGS][WARN] " << message << '\n';
}

void error(const std::string_view message) noexcept {
    std::cerr << "[OGS][ERROR] " << message << '\n';
}

} // namespace ogs::log

