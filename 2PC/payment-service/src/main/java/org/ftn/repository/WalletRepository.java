package org.ftn.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.ftn.entity.WalletEntity;

import java.util.UUID;

@ApplicationScoped
public class WalletRepository implements PanacheRepositoryBase<WalletEntity, UUID> {
}
