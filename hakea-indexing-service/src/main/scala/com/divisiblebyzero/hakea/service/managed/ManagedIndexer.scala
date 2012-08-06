package com.divisiblebyzero.hakea.service.managed

import com.divisiblebyzero.hakea.config.Configuration
import com.yammer.dropwizard.lifecycle.Managed

abstract class ManagedIndexer(configuration: Configuration) extends Managed
