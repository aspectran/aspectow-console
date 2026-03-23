/*
 * Copyright (c) 2020-present The Aspectran Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aspectran.aspectow.console.vault;

import com.aspectran.aspectow.console.common.db.model.Vault;
import com.aspectran.aspectow.console.common.service.VaultService;
import com.aspectran.core.component.bean.annotation.Action;
import com.aspectran.core.component.bean.annotation.Autowired;
import com.aspectran.core.component.bean.annotation.Component;
import com.aspectran.core.component.bean.annotation.Dispatch;
import com.aspectran.core.component.bean.annotation.Request;
import com.aspectran.core.component.bean.annotation.RequestToPost;
import com.aspectran.utils.StringUtils;
import com.aspectran.web.activity.response.RestResponse;
import com.aspectran.web.support.rest.response.FailureResponse;
import com.aspectran.web.support.rest.response.SuccessResponse;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Handles vault management requests.
 */
@Component("/vault")
public class VaultActivity {

    private final VaultService vaultService;

    @Autowired
    public VaultActivity(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @Request("/")
    @Dispatch("vault/list")
    @Action("page")
    public Map<String, Object> list() {
        List<Vault> vaultList = vaultService.getVaultList();
        return Map.of(
            "title", "Vault",
            "style", "vault-page",
            "include", "vault",
            "vaultList", vaultList,
            "now", LocalDateTime.now()
        );
    }

    @RequestToPost("/save")
    public RestResponse save(@NonNull Vault vault, String plainText, Integer expirationMinutes) {
        if (StringUtils.isEmpty(vault.getLabel())) {
            return new FailureResponse().setError("required", "Label is required.");
        }

        if (expirationMinutes != null && expirationMinutes > 0) {
            vault.setValidUntil(LocalDateTime.now().plusMinutes(expirationMinutes));
        }

        try {
            if (vault.getVaultId() != null) {
                // Update
                Vault existing = vaultService.getVaultById(vault.getVaultId());
                if (existing == null) {
                    return new FailureResponse().setError("not_found", "Token not found.");
                }
                vaultService.updateVault(vault, plainText);
                return new SuccessResponse("Updated").ok();
            } else {
                // Create
                if (StringUtils.isEmpty(plainText)) {
                    return new FailureResponse().setError("required", "Value to encrypt is required.");
                }
                vaultService.createVault(vault, plainText);
                return new SuccessResponse("Created").ok();
            }
        } catch (Exception e) {
            return new FailureResponse().setError("error", e.getMessage());
        }
    }

    @RequestToPost("/delete")
    public RestResponse delete(Long vaultId) {
        if (vaultId == null) {
            return new FailureResponse().setError("required", "Vault ID is required.");
        }
        vaultService.deleteVault(vaultId);
        return new SuccessResponse("Deleted").ok();
    }

    @RequestToPost("/decrypt")
    public RestResponse decrypt(String encryptedValue, String tokenType) {
        if (StringUtils.isEmpty(encryptedValue)) {
            return new FailureResponse().setError("required", "Encrypted value is required.");
        }
        try {
            String decrypted = vaultService.decrypt(encryptedValue, tokenType);
            return new SuccessResponse(decrypted).ok();
        } catch (Exception e) {
            return new FailureResponse().setError("failed", e.getMessage());
        }
    }

}
