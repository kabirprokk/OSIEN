// OGS Engine | version 0.2.0 | Deterministic fixed-step clock
#pragma once

#include <cstdint>

namespace ogs {

class FixedClock {
public:
    static constexpr double kStepSeconds = 1.0 / 120.0;
    static constexpr std::uint32_t kMaxStepsPerFrame = 8;

    void reset(double now_seconds = 0.0) noexcept;
    std::uint32_t advance(double now_seconds) noexcept;

    [[nodiscard]] double step_seconds() const noexcept;
    [[nodiscard]] double interpolation_alpha() const noexcept;
    [[nodiscard]] std::uint64_t total_steps() const noexcept;

private:
    double previous_seconds_ = 0.0;
    double accumulator_seconds_ = 0.0;
    std::uint64_t total_steps_ = 0;
};

} // namespace ogs
