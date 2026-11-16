// OGS Engine | version 0.2.0 | Deterministic fixed-step clock
#include "core/fixed_clock.h"

#include <algorithm>

namespace ogs {

void FixedClock::reset(const double now_seconds) noexcept {
    previous_seconds_ = now_seconds;
    accumulator_seconds_ = 0.0;
    total_steps_ = 0;
}

std::uint32_t FixedClock::advance(const double now_seconds) noexcept {
    double frame_seconds = now_seconds - previous_seconds_;
    previous_seconds_ = now_seconds;
    frame_seconds = std::clamp(frame_seconds, 0.0, kStepSeconds * kMaxStepsPerFrame);
    accumulator_seconds_ += frame_seconds;

    std::uint32_t steps = 0;
    while (accumulator_seconds_ >= kStepSeconds && steps < kMaxStepsPerFrame) {
        accumulator_seconds_ -= kStepSeconds;
        ++steps;
        ++total_steps_;
    }
    return steps;
}

double FixedClock::step_seconds() const noexcept {
    return kStepSeconds;
}

double FixedClock::interpolation_alpha() const noexcept {
    return accumulator_seconds_ / kStepSeconds;
}

std::uint64_t FixedClock::total_steps() const noexcept {
    return total_steps_;
}

} // namespace ogs
