# Stock subsys_radio compatibility interface

This module reconstructs the incompatible V1 contract shipped in the OnePlus 15
COS17 dump. Stock reports V1, but its transaction IDs differ from the existing
subsys_radio versions. It is not a compatible V10 update.

Keep the original module and its frozen API snapshots unchanged. The separate
build module retains the stock Binder package, descriptors, method signatures,
transaction IDs and parcel layouts. It imports subsys V5, as stock does. Only
the affected device selects it through its radio service dependency fixup.

Both modules define the same C++ types. Do not link the legacy and compatibility
libraries into the same process. Separate module names do not remove this ABI
constraint.

## Validation and API lifecycle

The reconstruction is in development. The current API dump is generated with
AIDL tooling; no released API version is claimed yet. Before release, use the
standard vendor.oplus.hardware.subsys_radio-compat-freeze-api target to create
the versioned snapshot and hash, then validate the device dependency graph and
runtime behavior. Do not manually substitute the OEM hash or bypass API checks.

Seven enum field types erased by the Java backend were recovered using its
default-initialization behavior and the matching stock enum definitions.

Static checks cover 710 method signatures, transaction IDs and oneway flags,
and 177 parcelable writers. The provisional generated API hash differs from
stock. Native readers and constructors have not been independently audited;
package build, linker and runtime validation remain pending. Preserve stock
names and wire types even where they differ from new-API style guidance.

The stock arm64 dependency scan found one direct consumer, libqti-radio-service,
and no older radio interface in its 66-library DT_NEEDED closure. This excludes
neither dynamic loading nor differences in the final LineageOS dependency graph.

A focused consumer review found OEM hash constant initialization, not a hash
rejection check. The consumer and generated library define the same inline hash
globals, so the effective returned hash must be verified after linking. A static
negative search is not a guarantee that every client accepts a different hash.
