// OGS Engine | browser-based scene runner
#pragma once

#include "editor/document.h"

#include <string>

namespace ogs::editor {

// Creates a self-contained playable HTML preview for the current scene.
[[nodiscard]] std::string make_browser_game_html(const Document& document);

} // namespace ogs::editor
