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

package info.pithos.monetization.service.relational;

import info.pithos.data.relational.FilterCriteria;
import info.pithos.data.relational.client.ProtoBufImmutableService;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.monetization.model.Monetization;
import info.pithos.monetization.service.FeatureService;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RelationalFeatureService extends ProtoBufImmutableService<Monetization.Feature>
        implements FeatureService {

    public RelationalFeatureService(RelationalClient relationalClient) {
        super(relationalClient, Monetization.Feature.getDefaultInstance());
    }

    @Override
    public CompletableFuture<List<Monetization.Feature>> listByApp(RequestContext rc, String appId) {
        return query(rc, FilterCriteria.eq("appId", appId).orderBy("name"));
    }
}
