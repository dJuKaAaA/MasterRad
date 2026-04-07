package org.ftn.consts;

import java.util.HashMap;
import java.util.Map;

public class Utils {
    private static final Map<String, String> scripts;

    static {
        scripts = new HashMap<>();
        scripts.put("order", getOrderDBTestValuesScript());
        scripts.put("inventory", getInventoryDBTestValuesScript());
        scripts.put("payment", getPaymentDBTestValuesScript());
    }

    public static String getKeycloakUrl() {
        return "http://host.docker.internal:9000/realms/distributed-transactions";
    }

    public static String getDBTestValuesScript(String service) {
        return scripts.get(service);
    }

    private static String getOrderDBTestValuesScript() {
        return """
                insert into order_entity (
                                   id,
                                   product_id,
                                   quantity,
                                   user_id,
                                   created_at,
                                   status
                               ) values (
                                            '03b229a1-0529-4a4a-a920-a7dda2637f70',
                                            'd572df76-b527-4e31-8aa3-9aa954d17100',
                                            2,
                                            '19e5ddb1-4c66-4d17-ad06-e8a6af23ed58',
                                            '2025-12-12T12:00:00Z',
                                            'COMPLETE'
                                        );
                               insert into order_entity (
                                   id,
                                   product_id,
                                   quantity,
                                   user_id,
                                   created_at,
                                   status
                               ) values (
                                            '03b229a1-0529-4a4a-a920-a7dda2637f71',
                                            'd572df76-b527-4e31-8aa3-9aa954d17101',
                                            3,
                                            '19e5ddb1-4c66-4d17-ad06-e8a6af23ed58',
                                            '2025-10-13T14:03:00Z',
                                            'COMPLETE'
                                        );
                               insert into order_entity (
                                   id,
                                   product_id,
                                   quantity,
                                   user_id,
                                   created_at,
                                   status
                               ) values (
                                            '03b229a1-0529-4a4a-a920-a7dda2637f72',
                                            'd572df76-b527-4e31-8aa3-9aa954d17102',
                                            5,
                                            '19e5ddb1-4c66-4d17-ad06-e8a6af23ed58',
                                            '2025-11-20T20:30:00Z',
                                            'COMPLETE'
                                        );
                               insert into order_entity (
                                   id,
                                   product_id,
                                   quantity,
                                   user_id,
                                   created_at,
                                   status
                               ) values (
                                            '03b229a1-0529-4a4a-a920-a7dda2637f73',
                                            'd572df76-b527-4e31-8aa3-9aa954d17103',
                                            4,
                                            '19e5ddb1-4c66-4d17-ad06-e8a6af23ed58',
                                            '2025-12-01T13:13:00Z',
                                            'COMPLETE'
                                        );
                               insert into order_entity (
                                   id,
                                   product_id,
                                   quantity,
                                   user_id,
                                   created_at,
                                   status
                               ) values (
                                            '03b229a1-0529-4a4a-a920-a7dda2637f74',
                                            'd572df76-b527-4e31-8aa3-9aa954d17104',
                                            1,
                                            '19e5ddb1-4c66-4d17-ad06-e8a6af23ed58',
                                            '2025-11-17T20:34:00Z',
                                            'COMPLETE'
                                        );""";
    }

    private static String getInventoryDBTestValuesScript() {
        return """
                INSERT INTO product_entity (
                                   id,
                                   name,
                                   description,
                                   price,
                                   added_at,
                                   status,
                                   merchant_id
                               ) VALUES (
                                            'd572df76-b527-4e31-8aa3-9aa954d17100',
                                            'Gaming Mouse',
                                            'High precision RGB gaming mouse',
                                            59.99,
                                            '2024-05-15T16:22:00Z',
                                            'ACTIVE',
                                            '76347922-6f4f-41df-8ff5-dae6bb66b69a'
                                        );
                               INSERT INTO inventory_entity (
                                   id,
                                   product_id,
                                   available_stock,
                                   created_at,
                                   last_updated_at,
                                   reserved_amount
                               ) VALUES (
                                            'a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c10',
                                            'd572df76-b527-4e31-8aa3-9aa954d17100',
                                            100,
                                            '2024-05-15T16:22:00Z',
                                            '2024-05-15T16:22:00Z',
                                            0
                                        );
                
                               INSERT INTO product_entity (
                                   id,
                                   name,
                                   description,
                                   price,
                                   added_at,
                                   status,
                                   merchant_id
                               ) VALUES (
                                            'd572df76-b527-4e31-8aa3-9aa954d17101',
                                            'Gaming Keyboard',
                                            'High precision RGB gaming keyboard',
                                            129.99,
                                            '2024-05-16T07:18:00Z',
                                            'ACTIVE',
                                            '76347922-6f4f-41df-8ff5-dae6bb66b69a'
                                        );
                               INSERT INTO inventory_entity (
                                   id,
                                   product_id,
                                   available_stock,
                                   created_at,
                                   last_updated_at,
                                   reserved_amount
                               ) VALUES (
                                            'a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c11',
                                            'd572df76-b527-4e31-8aa3-9aa954d17101',
                                            150,
                                            '2024-05-16T07:18:00Z',
                                            '2024-05-16T07:18:00Z',
                                            0
                                        );
                
                               INSERT INTO product_entity (
                                   id,
                                   name,
                                   description,
                                   price,
                                   added_at,
                                   status,
                                   merchant_id
                               ) VALUES (
                                            'd572df76-b527-4e31-8aa3-9aa954d17102',
                                            'XBOX Controller for PC',
                                            'XBOX Controller built for PC use',
                                            89.99,
                                            '2024-05-17T10:41:00Z',
                                            'ACTIVE',
                                            '76347922-6f4f-41df-8ff5-dae6bb66b69a'
                                        );
                               INSERT INTO inventory_entity (
                                   id,
                                   product_id,
                                   available_stock,
                                   created_at,
                                   last_updated_at,
                                   reserved_amount
                               ) VALUES (
                                            'a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c12',
                                            'd572df76-b527-4e31-8aa3-9aa954d17102',
                                            200,
                                            '2024-05-17T10:41:00Z',
                                            '2024-05-17T10:41:00Z',
                                            0
                                        );
                
                               INSERT INTO product_entity (
                                   id,
                                   name,
                                   description,
                                   price,
                                   added_at,
                                   status,
                                   merchant_id
                               ) VALUES (
                                            'd572df76-b527-4e31-8aa3-9aa954d17103',
                                            'Gaming Monitor',
                                            'Gaming Monitor with 300hz refresh rate with 4k resolution',
                                            599.99,
                                            '2024-05-20T17:27:00Z',
                                            'ACTIVE',
                                            '76347922-6f4f-41df-8ff5-dae6bb66b69a'
                                        );
                               INSERT INTO inventory_entity (
                                   id,
                                   product_id,
                                   available_stock,
                                   created_at,
                                   last_updated_at,
                                   reserved_amount
                               ) VALUES (
                                            'a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c13',
                                            'd572df76-b527-4e31-8aa3-9aa954d17103',
                                            20,
                                            '2024-05-20T17:27:00Z',
                                            '2024-05-20T17:27:00Z',
                                            0
                                        );
                
                               INSERT INTO product_entity (
                                   id,
                                   name,
                                   description,
                                   price,
                                   added_at,
                                   status,
                                   merchant_id
                               ) VALUES (
                                            'd572df76-b527-4e31-8aa3-9aa954d17104',
                                            'Gaming PC',
                                            'PC with Ryzen 7 CPU, RTX 3060 6GB graphics card, 32GB ram and 1TB of SSD',
                                            2099.99,
                                            '2024-06-01T06:21:00Z',
                                            'ACTIVE',
                                            '76347922-6f4f-41df-8ff5-dae6bb66b69a'
                                        );
                               INSERT INTO inventory_entity (
                                   id,
                                   product_id,
                                   available_stock,
                                   created_at,
                                   last_updated_at,
                                   reserved_amount
                               ) VALUES (
                                            'a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c14',
                                            'd572df76-b527-4e31-8aa3-9aa954d17104',
                                            25,
                                            '2024-06-01T06:21:00Z',
                                            '2024-06-01T06:21:00Z',
                                            0
                                        );
                
                               INSERT INTO product_entity (
                                   id,
                                   name,
                                   description,
                                   price,
                                   added_at,
                                   status,
                                   merchant_id
                               ) VALUES (
                                            'd572df76-b527-4e31-8aa3-9aa954d17105',
                                            'RAM 32gb',
                                            '32gb of RAM',
                                            999.99,
                                            '2025-11-29T16:21:00Z',
                                            'DISCONTINUED',
                                            '76347922-6f4f-41df-8ff5-dae6bb66b69a'
                                        );
                               INSERT INTO inventory_entity (
                                   id,
                                   product_id,
                                   available_stock,
                                   created_at,
                                   last_updated_at,
                                   reserved_amount
                               ) VALUES (
                                            'a3c1d2f5-4e6b-4b3f-9a2c-1e7d3f6b8c15',
                                            'd572df76-b527-4e31-8aa3-9aa954d17105',
                                            25,
                                            '2025-11-29T16:21:00Z',
                                            '2025-11-29T16:21:00Z',
                                            0
                                        );""";
    }

    private static String getPaymentDBTestValuesScript() {
        return """
                INSERT INTO wallet_entity (
                    id,
                    user_id,
                    balance
                ) VALUES (
                             'd1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20',
                             '19e5ddb1-4c66-4d17-ad06-e8a6af23ed58',
                             10000.0
                         );
                
                INSERT INTO payment_entity (
                    id,
                    price,
                    product_id,
                    product_quantity,
                    payed_at,
                    status,
                    payer_id,
                    total_price
                ) VALUES (
                             'e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d60',
                             59.99,
                             'd572df76-b527-4e31-8aa3-9aa954d17100',
                             2,
                             '2025-12-12T12:01:00Z',
                             'SUCCESS',
                             'd1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20',
                             119.98
                         );
                
                INSERT INTO payment_entity (
                    id,
                    price,
                    product_id,
                    product_quantity,
                    payed_at,
                    status,
                    payer_id,
                    total_price
                ) VALUES (
                             'e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d61',
                             129.99,
                             'd572df76-b527-4e31-8aa3-9aa954d17101',
                             3,
                             '2025-10-13T14:04:00Z',
                             'SUCCESS',
                             'd1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20',
                             389.97
                         );
                
                INSERT INTO payment_entity (
                    id,
                    price,
                    product_id,
                    product_quantity,
                    payed_at,
                    status,
                    payer_id,
                    total_price
                ) VALUES (
                             'e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d62',
                             89.99,
                             'd572df76-b527-4e31-8aa3-9aa954d17102',
                             5,
                             '2025-11-20T20:31:00Z',
                             'SUCCESS',
                             'd1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20',
                             449.95
                         );
                
                INSERT INTO payment_entity (
                    id,
                    price,
                    product_id,
                    product_quantity,
                    payed_at,
                    status,
                    payer_id,
                    total_price
                ) VALUES (
                             'e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d63',
                             599.99,
                             'd572df76-b527-4e31-8aa3-9aa954d17103',
                             4,
                             '2025-12-01T13:14:00Z',
                             'SUCCESS',
                             'd1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20',
                             2399.96
                         );
                
                INSERT INTO payment_entity (
                    id,
                    price,
                    product_id,
                    product_quantity,
                    payed_at,
                    status,
                    payer_id,
                    total_price
                ) VALUES (
                             'e3b2c1d4-7f5a-4c3b-8d1e-9b2c3a4f5d64',
                             2099.99,
                             'd572df76-b527-4e31-8aa3-9aa954d17104',
                             1,
                             '2025-11-17T20:35:00Z',
                             'SUCCESS',
                             'd1f3c2b4-5a7f-4b3d-9c1e-2f7a3d5b8c20',
                             2099.99
                         );""";
    }

}
