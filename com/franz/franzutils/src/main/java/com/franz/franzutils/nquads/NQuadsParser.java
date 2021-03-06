package com.franz.franzutils.nquads;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.sail.Sail;
import org.openrdf.sail.nativerdf.NativeStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
 * 1) "final" removed from NTriplesParser.getRDFFormat
 * 2) private member variables made public: reader, lineno, subject, predcate, object
 * 3) private methods: skipWhitespace, skipLine, parseSubject, parsePredicate, parseObject, throwEOFException
 * <p/>
 * Author: josh
 * Date: May 18, 2009
 * Time: 6:11:51 PM
 */
public class NQuadsParser extends OtherNTriplesParser {
    protected Resource context;

    // FIXME: delete me
    public static void main(final String[] args) throws Exception {
        String baseURI = "http://example.org/bogusBaseURI/";

        Sail sail = new NativeStore(new File("/tmp/btcSmallNativeStore"));
        sail.initialize();
        try {
            Repository repo = new SailRepository(sail);
            RepositoryConnection conn = repo.getConnection();
            try {
                InputStream is = new FileInputStream(
                        new File("/Users/josh/datasets/btc/btc-2009-small.nq"));
                try {
                    RDFParser parser = new NQuadsParser();
                    parser.setRDFHandler(new RDFInserter(conn));
                    parser.parse(is, baseURI);
                } finally {
                    is.close();
                }
            } finally {
                conn.close();
            }
        } finally {
            sail.shutDown();
        }
    }

    @Override
    public RDFFormat getRDFFormat() {
        // TODO
        return null;
    }

    @Override
    public void parse(final InputStream inputStream,
                      final String baseURI) throws IOException, RDFParseException, RDFHandlerException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream can not be 'null'");
        }
        // Note: baseURI will be checked in parse(Reader, String)

        try {
            parse(new InputStreamReader(inputStream, "US-ASCII"), baseURI);
        }
        catch (UnsupportedEncodingException e) {
            // Every platform should support the US-ASCII encoding...
            throw new RuntimeException(e);
        }
    }

    @Override
    public void parse(final Reader reader,
                      final String baseURI) throws IOException, RDFParseException, RDFHandlerException {
        if (reader == null) {
            throw new IllegalArgumentException("Reader can not be 'null'");
        }
        if (baseURI == null) {
            throw new IllegalArgumentException("base URI can not be 'null'");
        }

        rdfHandler.startRDF();

        this.reader = reader;
        lineNo = 1;

        reportLocation(lineNo, 1);

        try {
            int c = reader.read();
            c = skipWhitespace(c);

            while (c != -1) {
                if (c == '#') {
                    // Comment, ignore
                    c = skipLine(c);
                } else if (c == '\r' || c == '\n') {
                    // Empty line, ignore
                    c = skipLine(c);
                } else {
                    c = parseQuad(c);
                }

                c = skipWhitespace(c);
            }
        }
        finally {
            clear();
        }

        rdfHandler.endRDF();
    }

    private int parseQuad(int c)
            throws IOException, RDFParseException, RDFHandlerException {
        c = parseSubject(c);

        c = skipWhitespace(c);

        c = parsePredicate(c);

        c = skipWhitespace(c);

        c = parseObject(c);

        c = skipWhitespace(c);

        // FIXME: context is not required
        c = parseContext(c);

        c = skipWhitespace(c);

        if (c == -1) {
            throwEOFException();
        } else if (c != '.') {
            reportFatalError("Expected '.', found: " + (char) c);
        }

        c = skipLine(c);

        Statement st = createStatement(subject, predicate, object, context);
        rdfHandler.handleStatement(st);

        subject = null;
        predicate = null;
        object = null;
        context = null;

        return c;
    }

    protected int parseContext(int c)
            throws IOException, RDFParseException {
        // FIXME: context (in N-Quads) can be a literal
        return parseSubject(c);
    }
}