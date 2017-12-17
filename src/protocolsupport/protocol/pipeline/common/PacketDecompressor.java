package protocolsupport.protocol.pipeline.common;

import java.text.MessageFormat;
import java.util.List;
import java.util.zip.DataFormatException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import protocolsupport.protocol.serializer.MiscSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.utils.netty.Decompressor;

public class PacketDecompressor extends ByteToMessageDecoder {

	private static final int maxPacketLength = 2 << 21;

	private final Decompressor decompressor = Decompressor.create();

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		decompressor.recycle();
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf from, List<Object> list) throws DataFormatException {
		if (!from.isReadable()) {
			return;
		}
		int uncompressedlength = VarNumberSerializer.readVarInt(from);
		if (uncompressedlength == 0) {
			list.add(from.readBytes(from.readableBytes()));
		} else {
			if (uncompressedlength > maxPacketLength) {
				throw new DecoderException(MessageFormat.format("Badly compressed packet - size of {0} is larger than protocol maximum of {1}", uncompressedlength, maxPacketLength));
			}
			list.add(Unpooled.wrappedBuffer(decompressor.decompress(MiscSerializer.readAllBytes(from), uncompressedlength)));
		}
	}

}