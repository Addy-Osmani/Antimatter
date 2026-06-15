import base64
from cryptography.hazmat.primitives.asymmetric import ed25519
import base58

class Ed25519Auth:
    """Legacy Ed25519 Authentication Handler for Challenge/Response."""
    
    def __init__(self, private_key_pem: str | None = None):
        if private_key_pem:
            # We assume private_key_pem is base64 encoded raw bytes for simplicity, 
            # or we can parse proper PEM. The existing system seems to just encode the raw bytes.
            try:
                raw_bytes = base64.b64decode(private_key_pem)
                if len(raw_bytes) == 32:
                    self._private_key = ed25519.Ed25519PrivateKey.from_private_bytes(raw_bytes)
                else:
                    # Try loading as PEM if it's not raw bytes
                    from cryptography.hazmat.primitives import serialization
                    self._private_key = serialization.load_pem_private_key(
                        raw_bytes, password=None
                    )
            except Exception:
                self._private_key = ed25519.Ed25519PrivateKey.generate()
        else:
            self._private_key = ed25519.Ed25519PrivateKey.generate()
            
        self.public_key_raw = self._private_key.public_key().public_bytes_raw()
        
    @property
    def pairing_token(self) -> str:
        """The base58 encoded public key used as the pairing token."""
        return base58.b58encode(self.public_key_raw).decode('utf-8')
        
    @property
    def private_key_base64(self) -> str:
        """Exports the private key as base64 for saving to config."""
        return base64.b64encode(self._private_key.private_bytes_raw()).decode('utf-8')
        
    def sign_challenge(self, challenge_b64: str) -> str:
        """Signs a base64 encoded challenge and returns a base64 encoded signature."""
        challenge_bytes = base64.b64decode(challenge_b64)
        signature = self._private_key.sign(challenge_bytes)
        return base64.b64encode(signature).decode('utf-8')
        
    def verify_token(self, token: str) -> bool:
        """Verifies if a provided base58 token matches our pairing token."""
        return token == self.pairing_token
