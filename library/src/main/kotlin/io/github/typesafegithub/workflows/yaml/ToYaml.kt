package io.github.typesafegithub.workflows.yaml

import io.github.typesafegithub.workflows.actions.actions.CheckoutV3
import io.github.typesafegithub.workflows.domain.Job
import io.github.typesafegithub.workflows.domain.RunnerType.UbuntuLatest
import io.github.typesafegithub.workflows.domain.Workflow
import io.github.typesafegithub.workflows.dsl.toBuilder
import io.github.typesafegithub.workflows.internal.findGitRoot
import io.github.typesafegithub.workflows.internal.relativeToAbsolute
import io.github.typesafegithub.workflows.yaml.Preamble.Just
import io.github.typesafegithub.workflows.yaml.Preamble.WithOriginalAfter
import io.github.typesafegithub.workflows.yaml.Preamble.WithOriginalBefore
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.invariantSeparatorsPathString

/**
 * Returns a YAML string representing the workflow given in the receiver.
 *
 * @receiver a workflow which needs to be written to a YAML string.
 *
 * @param addConsistencyCheck If true, adds an extra job that makes sure the Kotlin script defined in
 * [Workflow.sourceFile] produces exactly the same YAML as in [Workflow.targetFileName], and fails the whole workflow if
 * it's not the case. This parameter defaults to `true` if [Workflow.sourceFile] is set, otherwise defaults to `false`.
 * @param gitRootDir Path to the git root directory, used for building relative paths for the consistency check. Usually
 * there's no need to set it explicitly, unless testing the library. Leave unset if unsure.
 * @param preamble Allows customizing the comment at the beginning of the generated YAML by either passing an extra
 * string, or replacing the whole preamble.
 *
 * @return Workflow as YAML string.
 */
public fun Workflow.toYaml(
    addConsistencyCheck: Boolean = sourceFile != null,
    gitRootDir: Path? = sourceFile?.absolute()?.findGitRoot(),
    preamble: Preamble? = null,
): String {
    return generateYaml(
        addConsistencyCheck = addConsistencyCheck,
        useGitDiff = false,
        gitRootDir = gitRootDir,
        preamble,
    )
}

/**
 * Writes the workflow given in the receiver to a YAML string, under a path that is built this way:
 * `<git-repo-root>/.github/workflows/<[Workflow.targetFileName]>.yaml`.
 *
 * @receiver a workflow which needs to be written to the file.
 *
 * @param addConsistencyCheck If true, adds an extra job that makes sure the Kotlin script defined in
 * [Workflow.sourceFile] produces exactly the same YAML as in [Workflow.targetFileName], and fails the whole workflow if
 * it's not the case. This parameter defaults to `true` if [Workflow.sourceFile] is set, otherwise defaults to `false`.
 * @param gitRootDir Path to the git root directory, used for building relative paths for the consistency check. Usually
 * there's no need to set it explicitly, unless testing the library. Leave unset if unsure.
 * @param preamble Allows customizing the comment at the beginning of the generated YAML by either passing an extra
 * string, or replacing the whole preamble.
 */
public fun Workflow.writeToFile(
    addConsistencyCheck: Boolean = sourceFile != null,
    gitRootDir: Path? = sourceFile?.absolute()?.findGitRoot(),
    preamble: Preamble? = null,
) {
    checkNotNull(gitRootDir) {
        "gitRootDir must be specified explicitly when sourceFile is null"
    }

    checkNotNull(this.targetFileName) {
        "targetFileName must not be null"
    }

    val yaml = generateYaml(
        addConsistencyCheck = addConsistencyCheck,
        useGitDiff = true,
        gitRootDir = gitRootDir,
        preamble,
    )

    gitRootDir.resolve(".github").resolve("workflows").resolve(targetFileName).toFile().let {
        it.parentFile.mkdirs()
        it.writeText(yaml)
    }
}

private fun commentify(preamble: String): String {
    if (preamble.isEmpty()) return ""

    return preamble
        .lineSequence()
        .joinToString("\n", postfix = "\n\n") { "# $it".trimEnd() }
}

@Suppress("LongMethod")
private fun Workflow.generateYaml(
    addConsistencyCheck: Boolean,
    useGitDiff: Boolean,
    gitRootDir: Path?,
    preamble: Preamble?,
): String {
    val sourceFilePath = gitRootDir?.let {
        sourceFile?.relativeToAbsolute(gitRootDir)?.invariantSeparatorsPathString
    }

    val jobsWithConsistencyCheck = if (addConsistencyCheck) {
        check(gitRootDir != null && sourceFile != null) {
            "consistency check requires a valid sourceFile and Git root directory"
        }

        checkNotNull(targetFileName) {
            "consistency check requires a targetFileName"
        }

        val targetFilePath = gitRootDir.resolve(".github").resolve("workflows").resolve(targetFileName)
            .relativeToAbsolute(gitRootDir).invariantSeparatorsPathString

        val consistencyCheckJob = this.toBuilder().job(
            id = "check_yaml_consistency",
            name = "Check YAML consistency",
            runsOn = UbuntuLatest,
            condition = yamlConsistencyJobCondition,
        ) {
            uses("Check out", CheckoutV3())
            if (useGitDiff) {
                run(
                    "Execute script",
                    "rm '$targetFilePath' " +
                        "&& '$sourceFilePath'",
                )
                run(
                    "Consistency check",
                    "git diff --exit-code '$targetFilePath'",
                )
            } else {
                run(
                    "Consistency check",
                    "diff -u '$targetFilePath' " +
                        "<('$sourceFilePath')",
                )
            }
        }
        listOf(consistencyCheckJob) + jobs.map {
            it.copy(needs = it.needs + consistencyCheckJob)
        }
    } else {
        jobs
    }

    val originalPreamble = commentify(
        if (sourceFilePath != null) {
            """
            This file was generated using Kotlin DSL ($sourceFilePath).
            If you want to modify the workflow, please change the Kotlin file and regenerate this YAML file.
            Generated with https://github.com/typesafegithub/github-workflows-kt
            """.trimIndent()
        } else {
            """
            This file was generated using a Kotlin DSL.
            If you want to modify the workflow, please change the Kotlin source and regenerate this YAML file.
            Generated with https://github.com/typesafegithub/github-workflows-kt
            """.trimIndent()
        },
    )

    val computedPreamble = when (preamble) {
        is Just -> commentify(preamble.content)
        is WithOriginalAfter -> commentify(preamble.content) + originalPreamble
        is WithOriginalBefore -> originalPreamble + commentify(preamble.content)
        null -> originalPreamble
    }

    val workflowToBeSerialized = this.toYamlInternal(jobsWithConsistencyCheck)
    val workflowAsYaml = workflowToBeSerialized.toYaml()

    return computedPreamble + workflowAsYaml
}

@Suppress("SpreadOperator")
private fun Workflow.toYamlInternal(jobsWithConsistencyCheck: List<Job<*>>): Map<String, Any> =
    mapOfNotNullValues(
        "name" to name,
        "on" to on.triggersToYaml(),
        "concurrency" to concurrency?.let {
            mapOf(
                "group" to it.group,
                "cancel-in-progress" to it.cancelInProgress,
            )
        },
        "env" to env.ifEmpty { null },
        *_customArguments.toList().toTypedArray(),
        "jobs" to jobsWithConsistencyCheck.jobsToYaml(),
    )
