/*
 * Copyright 2026 Pithos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package info.pithos.rbac;

import info.pithos.runtime.core.metrics.InfraOperation;
import info.pithos.runtime.core.metrics.MetricsCommitter;
import info.pithos.runtime.model.metrics.Metrics.MetricEvent;
import info.pithos.runtime.model.metrics.Metrics.MetricUnit;
import info.pithos.runtime.model.protocol.Context.RequestContext;

// High-value RBAC operations emitted as service-tier (Tier 2) metrics.
// No componentId is set, so MetricEventBuilder routes events to ServiceMetricRaw / ServiceCounter.
public enum RbacOperation implements InfraOperation {
    PERMISSION_CHECK  ("rbac.permission.check"),
    ROLE_GRANT        ("rbac.role.grant"),
    ROLE_REVOKE       ("rbac.role.revoke"),
    ROLE_CHECK        ("rbac.role.check"),
    GROUP_MEMBER_CHECK("rbac.group.member.check"),
    APIKEY_REVOKE     ("rbac.apikey.revoke"),
    APIKEY_RESOLVE    ("rbac.apikey.resolve"),
    APIKEY_TOUCH      ("rbac.apikey.touch");

    private final String stem;
    RbacOperation(String stem) { this.stem = stem; }
    @Override public String stem() { return stem; }

    static void record(MetricsCommitter mc, RequestContext rc, RbacOperation op, long startMs, Throwable ex) {
        if (mc == null) return;
        long elapsed = System.currentTimeMillis() - startMs;
        mc.record(rc, MetricEvent.newBuilder()
                .setMetric(op.latency()).setUnit(MetricUnit.MS).setValue(elapsed).build());
        mc.record(rc, MetricEvent.newBuilder()
                .setMetric(InfraOperation.outcome(op, ex)).setUnit(MetricUnit.COUNT).setValue(1.0).build());
    }
}
