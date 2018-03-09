/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.state.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.ambari.server.stack.ModuleFileUnmarshaller;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.Lifecycle;
import org.apache.ambari.server.state.stack.upgrade.LifecycleType;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.CollectionUtils;

/**
 * Tests for the upgrade pack
 */
public class UpgradePackTest {

  @Test
  public void testLifecycleOrdering() throws Exception {
    Collection<LifecycleType> typeSet = LifecycleType.ordered();

    LifecycleType[] types = typeSet.toArray(new LifecycleType[typeSet.size()]);

    assertEquals(LifecycleType.INSTALL, types[0]);
    assertEquals(LifecycleType.FINALIZE, types[types.length-1]);
  }

  @Test
  public void testUpgradeParsing() throws Exception {
    File f = new File("src/test/resources/mpacks-v2/upgrade-packs/upgrade-basic.xml");

    ModuleFileUnmarshaller unmarshaller = new ModuleFileUnmarshaller();

    UpgradePack upgradepack = unmarshaller.unmarshal(UpgradePack.class, f, true);

    assertEquals(UpgradeType.ROLLING, upgradepack.getType());
    assertEquals(6, upgradepack.lifecycles.size());

    Optional<Lifecycle> upgradeLifecycle = upgradepack.lifecycles.stream()
        .filter(l -> l.type == LifecycleType.UPGRADE).findFirst();

    Optional<Lifecycle> startLifecycle = upgradepack.lifecycles.stream()
        .filter(l -> l.type == LifecycleType.START).findFirst();

    assertTrue(upgradeLifecycle.isPresent());
    assertFalse(startLifecycle.isPresent());

    List<Grouping> groups = upgradeLifecycle.get().groups;
    assertEquals(29, groups.size());
    assertEquals(LifecycleType.UPGRADE, groups.get(0).lifecycle);

    Optional<Grouping> optional = groups.stream().filter(g -> "Kafka".equals(g.title)).findFirst();
    assertTrue(optional.isPresent());
    Assert.assertNull(optional.get().name);

    assertEquals(12, upgradepack.getPrerequisiteChecks().size());

    PrerequisiteCheckConfig checkConfig = upgradepack.getPrerequisiteCheckConfig();
    Map<String, String> map = checkConfig.getCheckProperties("abc");
    assertTrue(CollectionUtils.isEmpty(map));

    map = checkConfig.getCheckProperties("org.apache.ambari.server.checks.HiveDynamicServiceDiscoveryCheck");
    assertFalse(CollectionUtils.isEmpty(map));
    assertTrue(map.containsKey("min-failure-stack-version"));
    assertTrue(map.containsKey("my-property"));
    assertFalse(map.containsKey("random-key"));
    assertEquals("HDP-2.3.0.0", map.get("min-failure-stack-version"));
    assertEquals("my-value", map.get("my-property"));
  }

}
