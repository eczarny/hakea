package com.divisiblebyzero.hakea.service.managed

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.yammer.dropwizard.lifecycle.Managed

abstract class ManagedIndexer(configuration: HakeaConfiguration) extends Managed
