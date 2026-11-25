// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

#include <stddef.h>
#include <stdint.h>

#ifdef _WIN32
#define PGM_API __declspec(dllimport)
#else
#define PGM_API __attribute__((visibility("default")))
#endif

typedef size_t PGM_Alias;
typedef int64_t PGM_Idx;
PGM_API PGM_Alias PGM_new_export(PGM_Idx const* ids);

#ifdef PGM_FIXTURE_VARIATION
PGM_API int PGM_conditional(void);
#endif

#ifdef PGM_FIXTURE_INVALID
typedef unsigned long PGM_Forbidden;
PGM_API int PGM_bad_parameter(PGM_Forbidden value);
PGM_API void PGM_bad_variadic(int count, ...);
#endif

#ifdef PGM_FIXTURE_EXPORTS
#ifdef _WIN32
__declspec(dllexport) int PGM_dll_export(void);
#else
#define PGM_HIDDEN __attribute__((visibility("hidden")))
PGM_HIDDEN int PGM_hidden(void);
#endif
#endif

#ifdef PGM_FIXTURE_UNANNOTATED
int PGM_unannotated(void);
#endif
