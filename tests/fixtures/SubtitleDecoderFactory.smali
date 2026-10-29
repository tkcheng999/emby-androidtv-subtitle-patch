# Hand-written structural fixture, not a copy of the upstream class.
.class public Lcom/google/android/exoplayer2/text/SubtitleDecoderFactory$1;
.method public createDecoder(Lcom/google/android/exoplayer2/Format;)Ljava/lang/Object;
    .locals 1
    new-instance v0, Lcom/google/android/exoplayer2/text/pgs/PgsDecoder;
    invoke-direct {v0}, Lcom/google/android/exoplayer2/text/pgs/PgsDecoder;-><init>()V
    return-object v0
.end method
.method public supportsFormat(Lcom/google/android/exoplayer2/Format;)Z
    .locals 1
    const/4 v0, 0x0
    return v0
.end method
