package com.divisiblebyzero.hakea.indexing.util

import java.io.File

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

object RepositoryConversions {

  implicit def stringToRepository(path: String): Repository =
    new FileRepositoryBuilder().setGitDir(new File(path)).readEnvironment().build()
}
