// OGS Engine | version 0.1.0 | Windows-first editor entry point
#include "core/application.h"

int main() {
    ogs::ApplicationConfig config{};
    config.title = "OSEVA Game Studio Editor";
    ogs::Application application(config);
    return application.run();
}

