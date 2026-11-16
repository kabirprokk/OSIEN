// OGS Engine | version 0.1.0 | Minimal structured logging
#pragma once

#include <string_view>

namespace ogs::log {

void info(std::string_view message) noexcept;
void warning(std::string_view message) noexcept;
void error(std::string_view message) noexcept;

} // namespace ogs::log

