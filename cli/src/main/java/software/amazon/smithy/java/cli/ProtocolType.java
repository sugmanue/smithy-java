package software.amazon.smithy.java.cli;

import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public enum ProtocolType {
    AWS_JSON,
    RPC_V2_CBOR,
    REST_JSON,
    REST_XML
}
