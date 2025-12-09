import Navbar from "@/src/components/Navbar";
import HeroSection from "@/src/components/HeroSection"
import BoatsSection from "@/src/components/BoatsSection";
import HowItWorksSection from "@/src/components/HowItWorksSection"
import TestimonialsSection from "@/src/components/TestimonialsSection";
import CTASection from "@/src/components/CTASection";
import Footer from "@/src/components/Footer";


export default function Home() {
  return (
    <div className="min-h-screen">
      <Navbar />
        <main>
          <HeroSection />
          <BoatsSection />
          <HowItWorksSection />
          <TestimonialsSection />
          <CTASection />
        </main>
      <Footer />
    </div>
  );
};
