// This file was generated using 'code-generator' module. Don't change it by hand, your changes will
// be overwritten with the next wrapper code regeneration. Instead, consider introducing changes to the
// generator itself.
@file:Suppress(
    "DataClassPrivateConstructor",
    "UNUSED_PARAMETER",
    "DEPRECATION",
)

package io.github.typesafegithub.workflows.actions.cachix

import io.github.typesafegithub.workflows.domain.actions.Action
import io.github.typesafegithub.workflows.domain.actions.RegularAction
import java.util.LinkedHashMap
import kotlin.Deprecated
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.toList
import kotlin.collections.toTypedArray

/**
 * Action: Install Nix
 *
 * Installs Nix on GitHub Actions for the supported platforms: Linux and macOS.
 *
 * [Action on GitHub](https://github.com/cachix/install-nix-action)
 */
@Deprecated(
    message = "This action has a newer major version: InstallNixActionV22",
    replaceWith = ReplaceWith("InstallNixActionV22"),
)
public data class InstallNixActionV18 private constructor(
    /**
     * Installation URL that will contain a script to install Nix.
     */
    public val installUrl: String? = null,
    /**
     * Additional installer flags passed to the installer script.
     */
    public val installOptions: List<String>? = null,
    /**
     * Set NIX_PATH environment variable.
     */
    public val nixPath: String? = null,
    /**
     * gets appended to `/etc/nix/nix.conf` if passed.
     */
    public val extraNixConfig: String? = null,
    /**
     * Type-unsafe map where you can put any inputs that are not yet supported by the wrapper
     */
    public val _customInputs: Map<String, String> = mapOf(),
    /**
     * Allows overriding action's version, for example to use a specific minor version, or a newer
     * version that the wrapper doesn't yet know about
     */
    public val _customVersion: String? = null,
) : RegularAction<Action.Outputs>("cachix", "install-nix-action", _customVersion ?: "v18") {
    public constructor(
        vararg pleaseUseNamedArguments: Unit,
        installUrl: String? = null,
        installOptions: List<String>? = null,
        nixPath: String? = null,
        extraNixConfig: String? = null,
        _customInputs: Map<String, String> = mapOf(),
        _customVersion: String? = null,
    ) : this(installUrl=installUrl, installOptions=installOptions, nixPath=nixPath,
            extraNixConfig=extraNixConfig, _customInputs=_customInputs,
            _customVersion=_customVersion)

    @Suppress("SpreadOperator")
    override fun toYamlArguments(): LinkedHashMap<String, String> = linkedMapOf(
        *listOfNotNull(
            installUrl?.let { "install_url" to it },
            installOptions?.let { "install_options" to it.joinToString("\n") },
            nixPath?.let { "nix_path" to it },
            extraNixConfig?.let { "extra_nix_config" to it },
            *_customInputs.toList().toTypedArray(),
        ).toTypedArray()
    )

    override fun buildOutputObject(stepId: String): Action.Outputs = Outputs(stepId)
}
